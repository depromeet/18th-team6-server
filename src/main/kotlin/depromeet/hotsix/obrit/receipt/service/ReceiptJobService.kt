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
     * 대기중 잡 하나를 처리한다. 저장된 이미지를 내려받아 OCR·조립 후 완료 처리하고,
     * 실패 시 잡을 실패 상태로 남긴다. (Stage 1: 단건 처리, 재시도/멈춤 회수 없음)
     */
    @Transactional
    fun processNextPending() {
        val job = receiptJobRepository.findFirstByStatusOrderByIdAsc(ReceiptJobStatus.PENDING) ?: return
        job.markProcessing()

        try {
            val imageBytes = fileUploader.download(job.imageKey)
            val ocrResult = ocrService.analyzeReceiptImage(imageBytes, job.mimeType)
            val response = receiptService.assembleResponse(job.userId, ocrResult, job.imageKey)
            job.markCompleted(objectMapper.writeValueAsString(response))
        } catch (e: Exception) {
            job.markFailed(e.message ?: "영수증 처리 중 오류가 발생했습니다.")
        }
    }
}
