package depromeet.hotsix.obrit.admin.service

import depromeet.hotsix.obrit.admin.dto.AdminCategoryForm
import depromeet.hotsix.obrit.admin.dto.AdminCategoryOption
import depromeet.hotsix.obrit.admin.dto.AdminCategoryRow
import depromeet.hotsix.obrit.admin.dto.AdminIconForm
import depromeet.hotsix.obrit.admin.dto.AdminIconOption
import depromeet.hotsix.obrit.admin.dto.AdminIconRow
import depromeet.hotsix.obrit.admin.dto.AdminItemForm
import depromeet.hotsix.obrit.admin.dto.AdminItemRow
import depromeet.hotsix.obrit.admin.dto.AdminReplacementForm
import depromeet.hotsix.obrit.admin.dto.AdminUserForm
import depromeet.hotsix.obrit.admin.dto.AdminUserRow
import depromeet.hotsix.obrit.category.entity.Category
import depromeet.hotsix.obrit.category.entity.CategoryIcon
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
        ?: throw ResourceNotFoundException("존재하지 않는 아이템입니다.")

    @Transactional(readOnly = true)
    fun listIcons(includeDeleted: Boolean): List<AdminIconRow> = categoryIconRepository.findAllByOrderByIdAsc()
        .filter { includeDeleted || it.deletedAt == null }
        .map {
            AdminIconRow(
                id = it.id,
                name = it.name,
                url = it.url,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt,
                deletedAt = it.deletedAt,
            )
        }

    @Transactional(readOnly = true)
    fun getIcon(iconId: Long): AdminIconRow = categoryIconRepository.findById(iconId)
        .orElseThrow { ResourceNotFoundException("존재하지 않는 아이콘입니다.") }
        .let {
            AdminIconRow(
                id = it.id,
                name = it.name,
                url = it.url,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt,
                deletedAt = it.deletedAt,
            )
        }

    @Transactional(readOnly = true)
    fun listIconOptions(): List<AdminIconOption> = categoryIconRepository.findAllByOrderByIdDesc()
        .filter { it.deletedAt == null }
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
            throw BusinessException("UUID는 필수입니다.")
        }
        if (form.name.isBlank()) {
            throw BusinessException("사용자 이름은 필수입니다.")
        }
        if (userRepository.findByUuidAndDeletedAtIsNull(form.uuid.trim()) != null) {
            throw BusinessException("이미 존재하는 UUID입니다.")
        }
        userRepository.save(User(uuid = form.uuid.trim(), name = form.name.trim()))
    }

    @Transactional
    fun createIcon(form: AdminIconForm) {
        validateIconForm(form)
        categoryIconRepository.save(CategoryIcon(name = form.name.trim(), key = "", url = form.url.trim()))
    }

    @Transactional
    fun updateIcon(iconId: Long, form: AdminIconForm) {
        validateIconForm(form)
        val icon = categoryIconRepository.findById(iconId)
            .orElseThrow { ResourceNotFoundException("존재하지 않는 아이콘입니다.") }
        icon.updateForAdmin(name = form.name, url = form.url)
    }

    @Transactional
    fun deleteIcon(iconId: Long) {
        val icon = categoryIconRepository.findById(iconId)
            .orElseThrow { ResourceNotFoundException("존재하지 않는 아이콘입니다.") }
        val inUse = categoryRepository.findAll()
            .any { it.deletedAt == null && it.iconId == iconId }
        if (inUse) {
            throw BusinessException("사용 중인 아이콘은 삭제할 수 없습니다.")
        }
        icon.softDelete()
    }

    @Transactional
    fun updateUserName(userId: Long, name: String) {
        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("존재하지 않는 사용자입니다.") }
        if (name.isBlank()) {
            throw BusinessException("사용자 이름은 필수입니다.")
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
            .orElseThrow { ResourceNotFoundException("존재하지 않는 아이템입니다.") }
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
            .orElseThrow { ResourceNotFoundException("존재하지 않는 아이템입니다.") }
        item.softDelete()
    }

    @Transactional
    fun recordReplacement(itemId: Long, form: AdminReplacementForm) {
        val item = itemRepository.findById(itemId)
            .orElseThrow { ResourceNotFoundException("존재하지 않는 아이템입니다.") }
        val replacedDate = form.replacedDate ?: LocalDate.now(clock)
        item.replace(replacedDate)
        itemReplacementHistoryRepository.save(ItemReplacementHistory(item = item, replacedDate = replacedDate))
    }

    private fun validateCategoryForm(form: AdminCategoryForm) {
        if (form.name.isBlank()) {
            throw BusinessException("카테고리 이름은 필수입니다.")
        }
        if (form.defaultReplacementIntervalDays <= 0) {
            throw BusinessException("기본 교체 주기는 1일 이상이어야 합니다.")
        }
        val icon = categoryIconRepository.findById(form.iconId)
            .orElseThrow { BusinessException("유효하지 않은 아이콘입니다.") }
        if (icon.deletedAt != null) {
            throw BusinessException("유효하지 않은 아이콘입니다.")
        }
        form.userId?.let {
            if (!userRepository.existsById(it)) {
                throw ResourceNotFoundException("존재하지 않는 사용자입니다.")
            }
        }
    }

    private fun validateIconForm(form: AdminIconForm) {
        if (form.name.isBlank()) {
            throw BusinessException("아이콘 이름은 필수입니다.")
        }
        if (form.url.isBlank()) {
            throw BusinessException("아이콘 URL은 필수입니다.")
        }
    }

    private fun validateItemForm(form: AdminItemForm) {
        if (form.name.isBlank()) {
            throw BusinessException("아이템 이름은 필수입니다.")
        }
        if (form.count < 0) {
            throw BusinessException("수량은 0 이상이어야 합니다.")
        }
        if (form.replacementIntervalDays <= 0) {
            throw BusinessException("교체 주기는 1일 이상이어야 합니다.")
        }
        if (!userRepository.existsById(form.userId)) {
            throw ResourceNotFoundException("존재하지 않는 사용자입니다.")
        }
        val category = categoryRepository.findById(form.categoryId)
            .orElseThrow { ResourceNotFoundException("존재하지 않는 소모품 카테고리입니다.") }
        if (category.deletedAt != null) {
            throw ResourceNotFoundException("존재하지 않는 소모품 카테고리입니다.")
        }
        if (category.userId != null && category.userId != form.userId) {
            throw BusinessException("사용자 카테고리 소유자가 일치하지 않습니다.")
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
