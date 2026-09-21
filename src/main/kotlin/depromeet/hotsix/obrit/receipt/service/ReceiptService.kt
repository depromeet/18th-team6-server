package depromeet.hotsix.obrit.receipt.service

import depromeet.hotsix.obrit.category.service.CategoryQueryService
import depromeet.hotsix.obrit.global.common.storage.FileUploader
import depromeet.hotsix.obrit.global.exception.BusinessException
import depromeet.hotsix.obrit.global.log.analytics.event.ReceiptAnalysisFailureReason
import depromeet.hotsix.obrit.global.log.analytics.event.ReceiptAnalysisFinishedEvent
import depromeet.hotsix.obrit.global.log.analytics.service.AnalyticsEventService
import depromeet.hotsix.obrit.receipt.client.OcrFailedException
import depromeet.hotsix.obrit.receipt.client.OcrFailureReason
import depromeet.hotsix.obrit.receipt.dto.AnalyzeReceiptResponse
import depromeet.hotsix.obrit.receipt.dto.AnalyzedItem
import depromeet.hotsix.obrit.receipt.dto.OcrItem
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeUnit

private const val RECEIPT_PREFIX = "receipts"
private const val MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024
private val EXTENSION_TO_MIME_TYPE = mapOf(
    "jpg" to "image/jpeg",
    "jpeg" to "image/jpeg",
    "png" to "image/png",
    "webp" to "image/webp",
    "heic" to "image/heic",
    "heif" to "image/heif",
)

@Service
class ReceiptService(
    private val fileUploader: FileUploader,
    private val ocrService: OcrService,
    private val categoryQueryService: CategoryQueryService,
    private val analyticsEventService: AnalyticsEventService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun analyzeReceipt(userId: Long, imageFile: MultipartFile): AnalyzeReceiptResponse {
        val startedAt = System.nanoTime()
        var ocrMs: Long? = null

        // 검증 실패는 OCR 호출 전에 끝나므로, 이 구간에서 난 예외는 INVALID_FILE로 구분한다.
        try {
            validateImageFile(imageFile)
        } catch (t: Throwable) {
            publishAnalysisFinished(
                userId = userId,
                success = false,
                totalMs = elapsedMsSince(startedAt),
                ocrMs = null,
                detectedItemCount = null,
                failureReason = ReceiptAnalysisFailureReason.INVALID_FILE,
            )
            throw t
        }

        try {
            val mimeType = resolveMimeType(imageFile)
            val ocrStartedAt = System.nanoTime()
            val ocrResult = try {
                ocrService.analyzeReceiptImage(imageFile.bytes, mimeType)
            } finally {
                ocrMs = elapsedMsSince(ocrStartedAt)
            }
            val uploadFuture = CompletableFuture.supplyAsync(
                { fileUploader.upload(RECEIPT_PREFIX, imageFile) },
                ForkJoinPool.commonPool(),
            )

            val categoryNameToId = categoryQueryService.findAccessibleCategoryNameToIdMap(userId)
            val matchedCategoryIds = ocrResult.items.mapNotNull { categoryNameToId[it.category] }
            val categoryIdToIconUrl = categoryQueryService.findVisibleCategoryIconUrls(userId, matchedCategoryIds)
            val defaultIconUrl = categoryQueryService.getDefaultCategoryIconUrl()

            val analyzedItems =
                buildAnalyzedItems(ocrResult.items, categoryNameToId, categoryIdToIconUrl, defaultIconUrl)
            val receiptImageUrl = awaitUpload(uploadFuture)

            publishAnalysisFinished(
                userId = userId,
                success = true,
                totalMs = elapsedMsSince(startedAt),
                ocrMs = ocrMs,
                detectedItemCount = analyzedItems.size,
                failureReason = null,
            )

            return AnalyzeReceiptResponse(
                receiptImageUrl = receiptImageUrl,
                purchasedDate = ocrResult.date,
                items = analyzedItems,
            )
        } catch (t: Throwable) {
            publishAnalysisFinished(
                userId = userId,
                success = false,
                totalMs = elapsedMsSince(startedAt),
                ocrMs = ocrMs,
                detectedItemCount = null,
                failureReason = resolveFailureReason(t),
            )
            throw t
        }
    }

    private fun elapsedMsSince(startedAtNanos: Long): Long =
        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos)

    private fun resolveFailureReason(t: Throwable): ReceiptAnalysisFailureReason = when (t) {
        is OcrFailedException -> when (t.reason) {
            OcrFailureReason.UPSTREAM_5XX -> ReceiptAnalysisFailureReason.UPSTREAM_5XX
            OcrFailureReason.TIMEOUT -> ReceiptAnalysisFailureReason.TIMEOUT
            OcrFailureReason.EMPTY_RESPONSE -> ReceiptAnalysisFailureReason.EMPTY_RESPONSE
            OcrFailureReason.UNKNOWN -> ReceiptAnalysisFailureReason.UNKNOWN
        }

        else -> ReceiptAnalysisFailureReason.UNKNOWN
    }

    /** 지표 적재 실패가 영수증 분석을 깨뜨리지 않도록 격리한다. */
    private fun publishAnalysisFinished(
        userId: Long,
        success: Boolean,
        totalMs: Long,
        ocrMs: Long?,
        detectedItemCount: Int?,
        failureReason: ReceiptAnalysisFailureReason?,
    ) {
        try {
            analyticsEventService.publish(
                ReceiptAnalysisFinishedEvent(
                    userId = userId,
                    success = success,
                    totalMs = totalMs,
                    ocrMs = ocrMs,
                    detectedItemCount = detectedItemCount,
                    failureReason = failureReason,
                ),
            )
        } catch (t: Throwable) {
            log.error("receipt_analysis_finished 이벤트 발행에 실패했습니다.", t)
        }
    }

    private fun resolveMimeType(imageFile: MultipartFile): String {
        val extension = imageFile.originalFilename!!.substringAfterLast('.').lowercase()
        return EXTENSION_TO_MIME_TYPE[extension]!!
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

    private fun validateImageFile(file: MultipartFile) {
        val originalFilename = file.originalFilename
            ?: throw BusinessException("파일명이 없습니다.")

        val extension = originalFilename.substringAfterLast('.').lowercase()
        if (extension !in EXTENSION_TO_MIME_TYPE) {
            throw BusinessException("허용되지 않은 파일 확장자입니다. (허용: jpg, jpeg, png, webp, heic, heif)")
        }

        if (file.size > MAX_IMAGE_SIZE_BYTES) {
            throw BusinessException("파일 크기가 10MB를 초과합니다.")
        }

        if (file.isEmpty) {
            throw BusinessException("빈 파일입니다.")
        }
    }
}
