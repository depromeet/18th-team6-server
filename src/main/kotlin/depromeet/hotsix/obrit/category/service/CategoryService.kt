package depromeet.hotsix.obrit.category.service

import depromeet.hotsix.obrit.category.dto.request.CreateCategoryRequest
import depromeet.hotsix.obrit.category.dto.response.CategoryIconResponse
import depromeet.hotsix.obrit.category.dto.response.CategoryResponse
import depromeet.hotsix.obrit.category.entity.Category
import depromeet.hotsix.obrit.category.entity.CategoryGroupType
import depromeet.hotsix.obrit.category.repository.CategoryGroupRepository
import depromeet.hotsix.obrit.category.repository.CategoryIconRepository
import depromeet.hotsix.obrit.category.repository.CategoryRepository
import depromeet.hotsix.obrit.global.common.CategoryItemCleaner
import depromeet.hotsix.obrit.global.common.storage.UrlResolver
import depromeet.hotsix.obrit.global.exception.BusinessException
import depromeet.hotsix.obrit.global.exception.ConflictException
import depromeet.hotsix.obrit.global.exception.ResourceNotFoundException
import depromeet.hotsix.obrit.user.service.UserService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository,
    private val categoryIconRepository: CategoryIconRepository,
    private val categoryGroupRepository: CategoryGroupRepository,
    private val userService: UserService,
    private val categoryItemCleaner: CategoryItemCleaner,
    @Qualifier("s3PublicUrlResolver") private val iconUrlResolver: UrlResolver,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    fun listCategoryIcons(): List<CategoryIconResponse> = categoryIconRepository.findAllByOrderByIdDesc().map {
        CategoryIconResponse(iconId = it.id, url = iconUrlResolver.resolve(it.key))
    }

    @Transactional
    fun createCategory(userId: Long, request: CreateCategoryRequest): CategoryResponse {
        userService.validateUserExist(userId)

        val trimmedName = request.name.trim()
        if (categoryRepository.existsByUserIdIsNullAndNameAndDeletedAtIsNull(trimmedName) ||
            categoryRepository.existsByUserIdAndNameAndDeletedAtIsNull(userId, trimmedName)
        ) {
            throw ConflictException("이미 등록된 소모품 종류입니다.")
        }

        // request.iconId는 구버전 앱 호환용으로 받기만 하고 쓰지 않는다.
        val category = Category(
            userId = userId,
            name = trimmedName,
            iconId = Category.USER_DEFAULT_ICON_ID,
            defaultReplacementIntervalDays = Category.USER_DEFAULT_REPLACEMENT_INTERVAL_DAYS,
            categoryGroupId = findUserCategoryGroupId(),
        )
        val iconKey = categoryIconRepository.findById(Category.USER_DEFAULT_ICON_ID).map { it.key }.orElse("")

        return categoryRepository.save(category).toResponse(iconUrlResolver.resolve(iconKey))
    }

    @Transactional
    fun deleteCategory(userId: Long, categoryId: Long) {
        val category = categoryRepository.findActiveById(categoryId)
            ?: throw ResourceNotFoundException("존재하지 않는 소모품 카테고리입니다.")

        if (category.isPreset) {
            throw BusinessException("제공되는 소모품 카테고리는 삭제할 수 없습니다.")
        }

        if (!category.isUserCategoryOf(userId)) {
            throw ResourceNotFoundException("존재하지 않는 소모품 카테고리입니다.")
        }

        category.softDelete()
        categoryItemCleaner.softDeleteActiveItemsByCategory(categoryId, userId)
    }

    private fun Category.toResponse(iconUrl: String): CategoryResponse = CategoryResponse(
        categoryId = requireNotNull(id),
        name = name,
        iconUrl = iconUrl,
        userId = userId,
        createdAt = createdAt,
        defaultReplacementIntervalDays = defaultReplacementIntervalDays,
        categoryGroupId = categoryGroupId,
    )

    @Transactional
    fun getOrCreateUserCategoryId(userId: Long, categoryName: String): Long {
        val trimmedName = categoryName.trim()
        val existingCategory = categoryRepository.findActiveByUserIdAndName(userId, trimmedName)
        if (existingCategory != null) {
            return existingCategory.id!!
        }

        return try {
            val newCategory = Category(
                userId = userId,
                name = trimmedName,
                iconId = Category.USER_DEFAULT_ICON_ID,
                defaultReplacementIntervalDays = Category.USER_DEFAULT_REPLACEMENT_INTERVAL_DAYS,
                categoryGroupId = findUserCategoryGroupId(),
            )
            categoryRepository.save(newCategory).id!!
        } catch (e: Exception) {
            val retryCategory = categoryRepository.findActiveByUserIdAndName(userId, trimmedName)
                ?: throw e
            retryCategory.id!!
        }
    }

    // '사용자' 카테고리 행이 없어도 소모품 등록은 막지 않는다. 카테고리를 비워 두고 운영에서 행을 넣도록 알린다.
    private fun findUserCategoryGroupId(): Long? {
        val groupId = categoryGroupRepository.findFirstByTypeAndDeletedAtIsNullOrderByIdAsc(CategoryGroupType.USER)?.id
        if (groupId == null) {
            log.warn("'사용자' 카테고리(type=USER)가 없어 사용자 소모품의 카테고리를 비워 둡니다.")
        }
        return groupId
    }
}
