package depromeet.hotsix.obrit.receipt.service

import depromeet.hotsix.obrit.category.service.CategoryQueryService
import depromeet.hotsix.obrit.global.common.storage.FileUploader
import depromeet.hotsix.obrit.global.exception.BusinessException
import depromeet.hotsix.obrit.receipt.dto.AnalyzeReceiptResponse
import depromeet.hotsix.obrit.receipt.dto.AnalyzedItem
import depromeet.hotsix.obrit.receipt.dto.OcrAnalysisResponse
import depromeet.hotsix.obrit.receipt.dto.OcrItem
import depromeet.hotsix.obrit.receipt.entity.ReceiptImage
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ForkJoinPool

@Service
class ReceiptService(
    private val fileUploader: FileUploader,
    private val ocrService: OcrService,
    private val categoryQueryService: CategoryQueryService,
) {

    fun analyzeReceipt(userId: Long, imageFile: MultipartFile): AnalyzeReceiptResponse {
        val image = ReceiptImage.from(imageFile.bytes, imageFile.originalFilename)

        val ocrResult = ocrService.analyzeReceiptImage(image.bytes, image.mimeType)
        val uploadFuture = CompletableFuture.supplyAsync(
            { fileUploader.upload(RECEIPT_PREFIX, imageFile) },
            ForkJoinPool.commonPool(),
        )

        val receiptImageUrl = awaitUpload(uploadFuture)
        return assembleResponse(userId, ocrResult, receiptImageUrl)
    }

    /**
     * OCR 분석 결과를 사용자의 카테고리 정보와 결합해 등록용 응답으로 조립한다.
     * 동기 분석과 비동기 워커가 공유한다.
     */
    fun assembleResponse(
        userId: Long,
        ocrResult: OcrAnalysisResponse,
        receiptImageUrl: String,
    ): AnalyzeReceiptResponse {
        val categoryNameToId = categoryQueryService.findAccessibleCategoryNameToIdMap(userId)
        val matchedCategoryIds = ocrResult.items.mapNotNull { categoryNameToId[it.category] }
        val categoryIdToIconUrl = categoryQueryService.findVisibleCategoryIconUrls(userId, matchedCategoryIds)
        val defaultIconUrl = categoryQueryService.getDefaultCategoryIconUrl()

        val analyzedItems = buildAnalyzedItems(ocrResult.items, categoryNameToId, categoryIdToIconUrl, defaultIconUrl)

        return AnalyzeReceiptResponse(
            receiptImageUrl = receiptImageUrl,
            purchasedDate = ocrResult.date,
            items = analyzedItems,
        )
    }

    private fun buildAnalyzedItems(
        ocrItems: List<OcrItem>,
        categoryNameToId: Map<String, Long>,
        categoryIdToIconUrl: Map<Long, String>,
        defaultIconUrl: String,
    ): List<AnalyzedItem> = ocrItems.map { ocrItem ->
        val categoryId = categoryNameToId[ocrItem.category]
        AnalyzedItem(
            originalName = ocrItem.original_name,
            suggestedName = ocrItem.original_name,
            categoryId = categoryId,
            iconUrl = categoryId?.let { categoryIdToIconUrl[it] } ?: defaultIconUrl,
            suggestedCategoryName = ocrItem.category,
            quantity = ocrItem.effective_quantity,
            suggestedReplacementIntervalDays = ocrItem.suggested_replacement_interval_days ?: 1,
        )
    }

    private fun awaitUpload(future: CompletableFuture<String>): String = try {
        future.join()
    } catch (e: CompletionException) {
        val cause = e.cause
        if (cause is BusinessException) throw cause
        throw BusinessException("영수증 이미지 업로드 중 오류가 발생했습니다.")
    }

    companion object {
        const val RECEIPT_PREFIX = "receipts"
    }
}
