package depromeet.hotsix.obrit.receipt.service

import depromeet.hotsix.obrit.category.service.CategoryQueryService
import depromeet.hotsix.obrit.global.common.storage.FileUploader
import depromeet.hotsix.obrit.global.exception.BusinessException
import depromeet.hotsix.obrit.receipt.dto.AnalyzeReceiptResponse
import depromeet.hotsix.obrit.receipt.dto.AnalyzedItem
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
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

    fun analyzeReceipt(userId: Long, imageFile: MultipartFile): AnalyzeReceiptResponse {
        validateImageFile(imageFile)

        val extension = imageFile.originalFilename?.substringAfterLast('.')?.lowercase() ?: "jpg"
        val mimeType = if (extension == "png") "image/png" else "image/jpeg"
        val ocrResult = ocrService.analyzeReceiptImage(imageFile.bytes, mimeType)

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

        val receiptImageUrl = try {
            uploadFuture.join()
        } catch (e: CompletionException) {
            val cause = e.cause
            if (cause is depromeet.hotsix.obrit.global.exception.BusinessException) throw cause
            throw depromeet.hotsix.obrit.global.exception.BusinessException("영수증 이미지 업로드 중 오류가 발생했습니다.")
        }

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
