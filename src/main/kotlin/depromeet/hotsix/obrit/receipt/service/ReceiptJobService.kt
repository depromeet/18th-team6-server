package depromeet.hotsix.obrit.receipt.service

import depromeet.hotsix.obrit.global.common.storage.FileUploader
import depromeet.hotsix.obrit.global.exception.ResourceNotFoundException
import depromeet.hotsix.obrit.receipt.client.BatchOcrImage
import depromeet.hotsix.obrit.receipt.dto.AnalyzeReceiptResponse
import depromeet.hotsix.obrit.receipt.dto.OcrAnalysisResponse
import depromeet.hotsix.obrit.receipt.dto.ReceiptJobResponse
import depromeet.hotsix.obrit.receipt.entity.ReceiptImage
import depromeet.hotsix.obrit.receipt.entity.ReceiptJob
import depromeet.hotsix.obrit.receipt.entity.ReceiptJobStatus
import depromeet.hotsix.obrit.receipt.repository.ReceiptJobRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import tools.jackson.databind.ObjectMapper
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime

@Service
class ReceiptJobService(
    private val receiptJobRepository: ReceiptJobRepository,
    private val fileUploader: FileUploader,
    private val ocrService: OcrService,
    private val receiptService: ReceiptService,
    private val objectMapper: ObjectMapper,
    private val clock: Clock,
) {

    /**
     * 영수증 이미지를 검증·저장하고 대기중(PENDING) 잡으로 큐에 등록한다.
     */
    fun enqueue(userId: Long, imageFile: MultipartFile): Long {
        val image = ReceiptImage.from(imageFile.bytes, imageFile.originalFilename)
        val imageKey = fileUploader.upload(ReceiptService.RECEIPT_PREFIX, imageFile)

        val job = ReceiptJob(userId = userId, imageKey = imageKey, mimeType = image.mimeType)
        return receiptJobRepository.save(job).id!!
    }

    /**
     * 분석 잡의 현재 상태와 결과를 조회한다. 완료된 잡은 저장된 결과를 역직렬화해 반환한다.
     */
    @Transactional(readOnly = true)
    fun getJob(jobId: Long): ReceiptJobResponse {
        val job = receiptJobRepository.findById(jobId)
            .orElseThrow { ResourceNotFoundException("영수증 분석 잡을 찾을 수 없습니다: $jobId") }

        val result = job.resultJson?.let { objectMapper.readValue(it, AnalyzeReceiptResponse::class.java) }
        return ReceiptJobResponse(
            jobId = job.id!!,
            status = job.status,
            result = result,
            errorMessage = job.errorMessage,
        )
    }

    /**
     * 대기중 잡을 최대 [maxBatch]개 선점해 PROCESSING으로 표시하고 잡 ID 목록을 반환한다. (pick 트랜잭션)
     * 실제 처리는 [processBatch]에서 별도 트랜잭션으로 수행하므로, 외부 폴링에서 PROCESSING이 관측되고
     * Gemini 호출 동안 DB 트랜잭션을 잡지 않는다.
     */
    @Transactional
    fun pickBatch(maxBatch: Int): List<Long> {
        val jobs = receiptJobRepository.findByStatusOrderByIdAsc(
            ReceiptJobStatus.PENDING,
            PageRequest.of(0, maxBatch),
        )
        jobs.forEach { it.markProcessing() }
        return jobs.map { it.id!! }
    }

    /**
     * 선점된 잡들을 한 번의 배치 OCR 호출로 처리한다. 응답을 receipt_id(=잡 ID)로 매핑해
     * 각 잡을 개별 완료/재시도한다. (process 트랜잭션)
     *
     * - 배치 호출 자체 실패(다운로드·API 오류): 전체 잡을 재시도/실패 처리.
     * - 부분 실패(특정 잡의 결과 누락): 해당 잡만 재시도/실패, 나머지는 완료.
     */
    @Transactional
    fun processBatch(jobIds: List<Long>) {
        val jobs = jobIds.map { findJob(it) }

        val resultsByReceiptId = try {
            val images = jobs.map { job ->
                BatchOcrImage(job.id.toString(), fileUploader.download(job.imageKey), job.mimeType)
            }
            ocrService.analyzeReceiptImages(images).results.associateBy { it.receiptId }
        } catch (e: Exception) {
            jobs.forEach { retryOrFail(it, e.message ?: "배치 처리 중 오류가 발생했습니다.") }
            return
        }

        jobs.forEach { job ->
            val result = resultsByReceiptId[job.id.toString()]
            if (result == null) {
                retryOrFail(job, "배치 응답에서 결과를 찾지 못했습니다. (receipt_id=${job.id})")
            } else {
                val ocrResult = OcrAnalysisResponse(result.store, result.date, result.items, result.total)
                val response = receiptService.assembleResponse(job.userId, ocrResult, job.imageKey)
                job.markCompleted(objectMapper.writeValueAsString(response))
            }
        }
    }

    /** 재시도 한도가 남았으면 다시 대기중으로 되돌리고, 소진되었으면 실패 처리한다. */
    private fun retryOrFail(job: ReceiptJob, message: String) {
        if (job.retryCount < MAX_RETRY) {
            job.retry(message)
        } else {
            job.markFailed(message)
        }
    }

    /**
     * 선점했던 잡들을 다시 대기중으로 되돌린다. (토큰 소비 실패 등 처리 진입 전 롤백용)
     */
    @Transactional
    fun releaseToPending(jobIds: List<Long>) {
        jobIds.forEach { findJob(it).markPending() }
    }

    /**
     * 처리 도중 서버가 종료되는 등으로 일정 시간 이상 PROCESSING에 멈춘 잡을 대기중으로 회수한다.
     * (SQS visibility timeout과 동일한 개념)
     */
    @Transactional
    fun recoverStuckProcessing() {
        val threshold = LocalDateTime.now(clock).minus(STUCK_TIMEOUT)
        receiptJobRepository.findAllByStatusAndUpdatedAtBefore(ReceiptJobStatus.PROCESSING, threshold)
            .forEach { it.markPending() }
    }

    private fun findJob(jobId: Long): ReceiptJob = receiptJobRepository.findById(jobId)
        .orElseThrow { ResourceNotFoundException("영수증 분석 잡을 찾을 수 없습니다: $jobId") }

    companion object {
        private const val MAX_RETRY = 3
        private val STUCK_TIMEOUT: Duration = Duration.ofMinutes(1)
    }
}
