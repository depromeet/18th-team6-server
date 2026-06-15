package depromeet.hotsix.obrit.receipt.service

import depromeet.hotsix.obrit.category.repository.CategoryRepository
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
private const val MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024 // 10MB
private val ALLOWED_EXTENSIONS = setOf("jpg", "jpeg", "png")

@Service
class ReceiptService(
    private val fileUploader: FileUploader,
    private val ocrService: OcrService,
    private val categoryRepository: CategoryRepository,
) {

    @Transactional(readOnly = true)
    fun analyzeReceipt(userId: Long, imageFile: MultipartFile): AnalyzeReceiptResponse {
        // 1. 이미지 검증
        validateImageFile(imageFile)

        // 2. OCR 분석 먼저 실행 (실패 시 S3 업로드 없음)
        val ocrResult = ocrService.analyzeReceiptImage(imageFile.bytes)

        // 3. S3 업로드 비동기 시작
        val uploadFuture = CompletableFuture.supplyAsync(
            {
                fileUploader.upload(RECEIPT_PREFIX, imageFile)
            },
            ForkJoinPool.commonPool(),
        )

        // 4. 카테고리 미리 로드 (N+1 쿼리 해결)
        val userCategories = categoryRepository.findActiveByUserId(userId)
        val presetCategories = categoryRepository.findActivePresets()

        // 5. 카테고리 매칭
        val analyzedItems = ocrResult.items.map { ocrItem ->
            val matchedCategoryId = matchCategory(ocrItem.category, userCategories, presetCategories)

            AnalyzedItem(
                originalName = ocrItem.original_name,
                suggestedName = ocrItem.original_name,
                categoryId = matchedCategoryId,
                suggestedCategoryName = ocrItem.category,
                quantity = ocrItem.effective_quantity,
                suggestedReplacementIntervalDays = ocrItem.suggested_replacement_interval_days ?: 1,
            )
        }

        // 6. S3 업로드 완료 대기
        val receiptImageUrl = uploadFuture.join()

        // 7. 응답 구성
        return AnalyzeReceiptResponse(
            receiptImageUrl = receiptImageUrl,
            purchasedDate = ocrResult.date,
            items = analyzedItems,
        )
    }

    private fun validateImageFile(file: MultipartFile) {
        // 파일명 확인
        val originalFilename = file.originalFilename
            ?: throw BusinessException("파일명이 없습니다.")

        // 확장자 검증
        val extension = originalFilename.substringAfterLast('.').lowercase()
        if (extension !in ALLOWED_EXTENSIONS) {
            throw BusinessException("허용되지 않은 파일 확장자입니다. (허용: jpg, jpeg, png)")
        }

        // 크기 검증
        if (file.size > MAX_IMAGE_SIZE_BYTES) {
            throw BusinessException("파일 크기가 10MB를 초과합니다.")
        }

        // 빈 파일 검증
        if (file.isEmpty) {
            throw BusinessException("빈 파일입니다.")
        }
    }

    private fun matchCategory(
        suggestedCategoryName: String,
        userCategories: List<depromeet.hotsix.obrit.category.entity.Category>,
        presetCategories: List<depromeet.hotsix.obrit.category.entity.Category>,
    ): Long? {
        // 사용자 카테고리 우선 매칭
        val userCategory = userCategories.firstOrNull { it.name == suggestedCategoryName }
        if (userCategory != null) {
            return userCategory.id
        }

        // 프리셋 카테고리 매칭
        val presetCategory = presetCategories.firstOrNull { it.name == suggestedCategoryName }
        return presetCategory?.id
    }
}
