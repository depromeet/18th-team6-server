package depromeet.hotsix.obrit.receipt.service

import depromeet.hotsix.obrit.category.service.CategoryQueryService
import depromeet.hotsix.obrit.global.common.storage.FileUploader
import depromeet.hotsix.obrit.global.exception.BusinessException
import depromeet.hotsix.obrit.receipt.dto.AnalyzeReceiptResponse
import depromeet.hotsix.obrit.receipt.dto.AnalyzedItem
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ForkJoinPool

private const val RECEIPT_PREFIX = "receipts"
private const val MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024
private val ALLOWED_EXTENSIONS = setOf("jpg", "jpeg", "png")

@Service
class ReceiptService(
    private val fileUploader: FileUploader,
    private val ocrService: OcrService,
    private val categoryQueryService: CategoryQueryService,
) {

    @Transactional(readOnly = true)
    fun analyzeReceipt(userId: Long, imageFile: MultipartFile): AnalyzeReceiptResponse {
        validateImageFile(imageFile)

        val ocrResult = ocrService.analyzeReceiptImage(imageFile.bytes)

        val uploadFuture = CompletableFuture.supplyAsync(
            { fileUploader.upload(RECEIPT_PREFIX, imageFile) },
            ForkJoinPool.commonPool(),
        )

        val categoryNameToId = categoryQueryService.findAccessibleCategoryNameToIdMap(userId)

        val analyzedItems = ocrResult.items.map { ocrItem ->
            AnalyzedItem(
                originalName = ocrItem.original_name,
                suggestedName = ocrItem.original_name,
                categoryId = categoryNameToId[ocrItem.category],
                suggestedCategoryName = ocrItem.category,
                quantity = ocrItem.effective_quantity,
                suggestedReplacementIntervalDays = ocrItem.suggested_replacement_interval_days ?: 1,
            )
        }

        val receiptImageUrl = uploadFuture.join()

        return AnalyzeReceiptResponse(
            receiptImageUrl = receiptImageUrl,
            purchasedDate = ocrResult.date,
            items = analyzedItems,
        )
    }

    private fun validateImageFile(file: MultipartFile) {
        val originalFilename = file.originalFilename
            ?: throw BusinessException("파일명이 없습니다.")

        val extension = originalFilename.substringAfterLast('.').lowercase()
        if (extension !in ALLOWED_EXTENSIONS) {
            throw BusinessException("허용되지 않은 파일 확장자입니다. (허용: jpg, jpeg, png)")
        }

        if (file.size > MAX_IMAGE_SIZE_BYTES) {
            throw BusinessException("파일 크기가 10MB를 초과합니다.")
        }

        if (file.isEmpty) {
            throw BusinessException("빈 파일입니다.")
        }
    }
}
