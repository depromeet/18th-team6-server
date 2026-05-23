package depromeet.hotsix.obrit.admin.service

import depromeet.hotsix.obrit.admin.dto.AdminCategoryForm
import depromeet.hotsix.obrit.admin.dto.AdminCategoryOption
import depromeet.hotsix.obrit.admin.dto.AdminCategoryRow
import depromeet.hotsix.obrit.admin.dto.AdminIconOption
import depromeet.hotsix.obrit.admin.dto.AdminItemForm
import depromeet.hotsix.obrit.admin.dto.AdminItemRow
import depromeet.hotsix.obrit.admin.dto.AdminReplacementForm
import depromeet.hotsix.obrit.admin.dto.AdminUserForm
import depromeet.hotsix.obrit.admin.dto.AdminUserRow
import depromeet.hotsix.obrit.category.entity.Category
import depromeet.hotsix.obrit.category.repository.CategoryIconRepository
import depromeet.hotsix.obrit.category.repository.CategoryRepository
import depromeet.hotsix.obrit.global.exception.BusinessException
import depromeet.hotsix.obrit.global.exception.ResourceNotFoundException
import depromeet.hotsix.obrit.item.entity.Item
import depromeet.hotsix.obrit.item.entity.ItemReplacementHistory
import depromeet.hotsix.obrit.item.repository.ItemReplacementHistoryRepository
import depromeet.hotsix.obrit.item.repository.ItemRepository
import depromeet.hotsix.obrit.user.entity.User
import depromeet.hotsix.obrit.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate

@Service
class AdminBackofficeService(
    private val userRepository: UserRepository,
    private val categoryRepository: CategoryRepository,
    private val categoryIconRepository: CategoryIconRepository,
    private val itemRepository: ItemRepository,
    private val itemReplacementHistoryRepository: ItemReplacementHistoryRepository,
    private val clock: Clock,
) {

    @Transactional(readOnly = true)
    fun listUsers(includeDeleted: Boolean): List<AdminUserRow> = userRepository.findAllByOrderByIdAsc()
        .filter { includeDeleted || it.deletedAt == null }
        .map { it.toRow() }

    @Transactional(readOnly = true)
    fun getUser(userId: Long): AdminUserRow = userRepository.findById(userId)
        .orElseThrow { ResourceNotFoundException("존재하지 않는 사용자입니다.") }
        .toRow()

    @Transactional(readOnly = true)
    fun listCategories(includeDeleted: Boolean): List<AdminCategoryRow> {
        val iconsById = categoryIconRepository.findAll().associateBy { it.id }
        val itemCounts = itemRepository.findAll()
            .filter { includeDeleted || it.deletedAt == null }
            .groupingBy { it.categoryId }
            .eachCount()

        return categoryRepository.findAllByOrderByIdAsc()
            .filter { includeDeleted || it.deletedAt == null }
            .map { category ->
                val icon = iconsById[category.iconId]
                AdminCategoryRow(
                    id = requireNotNull(category.id),
                    userId = category.userId,
                    scope = if (category.isPreset) "PRESET" else "USER",
                    name = category.name,
                    iconId = category.iconId,
                    iconUrl = icon?.url,
                    defaultReplacementIntervalDays = category.defaultReplacementIntervalDays,
                    itemCount = itemCounts[requireNotNull(category.id)] ?: 0,
                    createdAt = category.createdAt,
                    updatedAt = category.updatedAt,
                    deletedAt = category.deletedAt,
                )
            }
    }

    @Transactional(readOnly = true)
    fun getCategory(categoryId: Long): AdminCategoryRow = listCategories(includeDeleted = true)
        .firstOrNull { it.id == categoryId }
        ?: throw ResourceNotFoundException("존재하지 않는 소모품 카테고리입니다.")

    @Transactional(readOnly = true)
    fun listItems(includeDeleted: Boolean): List<AdminItemRow> {
        val categoriesById = categoryRepository.findAll().associateBy { requireNotNull(it.id) }
        return itemRepository.findAllByOrderByIdAsc()
            .filter { includeDeleted || it.deletedAt == null }
            .map { item ->
                AdminItemRow(
                    id = requireNotNull(item.id),
                    userId = item.userId,
                    categoryId = item.categoryId,
                    categoryName = categoriesById[item.categoryId]?.name ?: "(deleted category)",
                    name = item.name,
                    count = item.quantity,
                    replacementIntervalDays = item.replacementIntervalDays,
                    lastReplacedDate = item.lastReplacedDate,
                    nextReplacementDate = item.nextReplacementDate,
                    createdAt = item.createdAt,
                    updatedAt = item.updatedAt,
                    deletedAt = item.deletedAt,
                )
            }
    }

    @Transactional(readOnly = true)
    fun getItem(itemId: Long): AdminItemRow = listItems(includeDeleted = true)
        .firstOrNull { it.id == itemId }
        ?: throw ResourceNotFoundException("Item not found.")

    @Transactional(readOnly = true)
    fun listIcons(): List<AdminIconOption> = categoryIconRepository.findAllByOrderByIdDesc()
        .map { AdminIconOption(id = it.id, name = it.name, url = it.url) }

    @Transactional(readOnly = true)
    fun listCategoryOptions(): List<AdminCategoryOption> = categoryRepository.findAllByOrderByIdAsc()
        .filter { it.deletedAt == null }
        .map {
            AdminCategoryOption(
                id = requireNotNull(it.id),
                userId = it.userId,
                label = if (it.userId == null) "${it.name} (preset)" else "${it.name} (user ${it.userId})",
            )
        }

    @Transactional
    fun createUser(form: AdminUserForm) {
        if (form.uuid.isBlank()) {
            throw BusinessException("UUID is required.")
        }
        if (form.name.isBlank()) {
            throw BusinessException("User name is required.")
        }
        if (userRepository.findByUuid(form.uuid.trim()) != null) {
            throw BusinessException("이미 존재하는 UUID입니다.")
        }
        userRepository.save(User(uuid = form.uuid.trim(), name = form.name.trim()))
    }

    @Transactional
    fun updateUserName(userId: Long, name: String) {
        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("존재하지 않는 사용자입니다.") }
        if (name.isBlank()) {
            throw BusinessException("User name is required.")
        }
        user.updateName(name)
    }

    @Transactional
    fun deleteUser(userId: Long) {
        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("존재하지 않는 사용자입니다.") }
        user.softDelete()
        itemRepository.findAllByUserId(userId).forEach { it.softDelete() }
        categoryRepository.findAllByUserId(userId).forEach { it.softDelete() }
    }

    @Transactional
    fun createCategory(form: AdminCategoryForm) {
        validateCategoryForm(form)
        categoryRepository.save(
            Category(
                userId = form.userId,
                name = form.name.trim(),
                iconId = form.iconId,
                defaultReplacementIntervalDays = form.defaultReplacementIntervalDays,
            ),
        )
    }

    @Transactional
    fun updateCategory(categoryId: Long, form: AdminCategoryForm) {
        validateCategoryForm(form)
        val category = categoryRepository.findById(categoryId)
            .orElseThrow { ResourceNotFoundException("존재하지 않는 소모품 카테고리입니다.") }
        category.updateForAdmin(
            name = form.name,
            iconId = form.iconId,
            defaultReplacementIntervalDays = form.defaultReplacementIntervalDays,
        )
    }

    @Transactional
    fun deleteCategory(categoryId: Long) {
        val category = categoryRepository.findById(categoryId)
            .orElseThrow { ResourceNotFoundException("존재하지 않는 소모품 카테고리입니다.") }
        category.softDelete()
        itemRepository.findAllByCategoryId(categoryId).forEach { it.softDelete() }
    }

    @Transactional
    fun createItem(form: AdminItemForm) {
        validateItemForm(form)
        val lastReplacedDate = form.lastReplacedDate ?: LocalDate.now(clock)
        itemRepository.save(
            Item(
                userId = form.userId,
                categoryId = form.categoryId,
                name = form.name.trim(),
                quantity = form.count,
                replacementIntervalDays = form.replacementIntervalDays,
                lastReplacedDate = lastReplacedDate,
                nextReplacementDate = lastReplacedDate.plusDays(form.replacementIntervalDays.toLong()),
            ),
        )
    }

    @Transactional
    fun updateItem(itemId: Long, form: AdminItemForm) {
        validateItemForm(form)
        val item = itemRepository.findById(itemId)
            .orElseThrow { ResourceNotFoundException("Item not found.") }
        item.userId = form.userId
        item.categoryId = form.categoryId
        item.update(
            name = form.name,
            quantity = form.count,
            replacementIntervalDays = form.replacementIntervalDays,
            lastReplacedDate = form.lastReplacedDate,
        )
    }

    @Transactional
    fun deleteItem(itemId: Long) {
        val item = itemRepository.findById(itemId)
            .orElseThrow { ResourceNotFoundException("Item not found.") }
        item.softDelete()
    }

    @Transactional
    fun recordReplacement(itemId: Long, form: AdminReplacementForm) {
        val item = itemRepository.findById(itemId)
            .orElseThrow { ResourceNotFoundException("Item not found.") }
        val replacedDate = form.replacedDate ?: LocalDate.now(clock)
        item.replace(replacedDate)
        itemReplacementHistoryRepository.save(ItemReplacementHistory(item = item, replacedDate = replacedDate))
    }

    private fun validateCategoryForm(form: AdminCategoryForm) {
        if (form.name.isBlank()) {
            throw BusinessException("Category name is required.")
        }
        if (form.defaultReplacementIntervalDays <= 0) {
            throw BusinessException("Default replacement interval must be positive.")
        }
        if (!categoryIconRepository.existsById(form.iconId)) {
            throw BusinessException("유효하지 않은 아이콘입니다.")
        }
        form.userId?.let {
            if (!userRepository.existsById(it)) {
                throw ResourceNotFoundException("존재하지 않는 사용자입니다.")
            }
        }
    }

    private fun validateItemForm(form: AdminItemForm) {
        if (form.name.isBlank()) {
            throw BusinessException("Item name is required.")
        }
        if (form.count < 0) {
            throw BusinessException("Count must be zero or positive.")
        }
        if (form.replacementIntervalDays <= 0) {
            throw BusinessException("Replacement interval must be positive.")
        }
        if (!userRepository.existsById(form.userId)) {
            throw ResourceNotFoundException("존재하지 않는 사용자입니다.")
        }
        if (!categoryRepository.existsById(form.categoryId)) {
            throw ResourceNotFoundException("존재하지 않는 소모품 카테고리입니다.")
        }
    }

    private fun User.toRow(): AdminUserRow = AdminUserRow(
        id = requireNotNull(id),
        uuid = uuid,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}
