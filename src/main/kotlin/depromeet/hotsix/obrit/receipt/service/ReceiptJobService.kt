package depromeet.hotsix.obrit.receipt.service

import depromeet.hotsix.obrit.global.common.storage.FileUploader
import depromeet.hotsix.obrit.global.exception.ResourceNotFoundException
import depromeet.hotsix.obrit.receipt.dto.AnalyzeReceiptResponse
import depromeet.hotsix.obrit.receipt.dto.ReceiptJobResponse
import depromeet.hotsix.obrit.receipt.entity.ReceiptImage
import depromeet.hotsix.obrit.receipt.entity.ReceiptJob
import depromeet.hotsix.obrit.receipt.entity.ReceiptJobStatus
import depromeet.hotsix.obrit.receipt.repository.ReceiptJobRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import tools.jackson.databind.ObjectMapper

@Service
class ReceiptJobService(
    private val receiptJobRepository: ReceiptJobRepository,
    private val fileUploader: FileUploader,
    private val ocrService: OcrService,
    private val receiptService: ReceiptService,
    private val objectMapper: ObjectMapper,
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
     * 대기중 잡 하나를 선점해 PROCESSING으로 표시하고 잡 ID를 반환한다. (pick 트랜잭션)
     * 실제 처리는 [process]에서 별도 트랜잭션으로 수행하므로, 외부 폴링에서 PROCESSING이 관측되고
     * Gemini 호출 동안 DB 트랜잭션을 잡지 않는다.
     */
    @Transactional
    fun pickNextPending(): Long? {
        val job = receiptJobRepository.findFirstByStatusOrderByIdAsc(ReceiptJobStatus.PENDING) ?: return null
        job.markProcessing()
        return job.id
    }

    /**
     * 선점된 잡을 처리한다. 저장된 이미지를 내려받아 OCR·조립 후 완료 처리하고,
     * 실패 시 잡을 실패 상태로 남긴다. (process 트랜잭션)
     */
    @Transactional
    fun process(jobId: Long) {
        val job = findJob(jobId)

        try {
            val imageBytes = fileUploader.download(job.imageKey)
            val ocrResult = ocrService.analyzeReceiptImage(imageBytes, job.mimeType)
            val response = receiptService.assembleResponse(job.userId, ocrResult, job.imageKey)
            job.markCompleted(objectMapper.writeValueAsString(response))
        } catch (e: Exception) {
            job.markFailed(e.message ?: "영수증 처리 중 오류가 발생했습니다.")
        }
    }

    /**
     * 선점했던 잡을 다시 대기중으로 되돌린다. (토큰 소비 실패 등 처리 진입 전 롤백용)
     */
    @Transactional
    fun releaseToPending(jobId: Long) {
        findJob(jobId).markPending()
    }

    private fun findJob(jobId: Long): ReceiptJob = receiptJobRepository.findById(jobId)
        .orElseThrow { ResourceNotFoundException("영수증 분석 잡을 찾을 수 없습니다: $jobId") }
}
