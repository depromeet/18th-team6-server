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
import depromeet.hotsix.obrit.admin.dto.AdminSignupFunnelDropOffRow
import depromeet.hotsix.obrit.admin.dto.AdminSignupFunnelStepRow
import depromeet.hotsix.obrit.admin.dto.AdminSignupFunnelTimelineRow
import depromeet.hotsix.obrit.admin.dto.AdminSignupFunnelUserRow
import depromeet.hotsix.obrit.admin.dto.AdminSignupFunnelView
import depromeet.hotsix.obrit.admin.dto.AdminUserForm
import depromeet.hotsix.obrit.admin.dto.AdminUserRow
import depromeet.hotsix.obrit.category.entity.Category
import depromeet.hotsix.obrit.category.entity.CategoryIcon
import depromeet.hotsix.obrit.category.repository.CategoryIconRepository
import depromeet.hotsix.obrit.category.repository.CategoryRepository
import depromeet.hotsix.obrit.global.common.storage.FileUploader
import depromeet.hotsix.obrit.global.common.storage.UrlResolver
import depromeet.hotsix.obrit.global.exception.BusinessException
import depromeet.hotsix.obrit.global.exception.ResourceNotFoundException
import depromeet.hotsix.obrit.global.log.access.entity.ApiAccessLog
import depromeet.hotsix.obrit.global.log.access.repository.ApiAccessLogRepository
import depromeet.hotsix.obrit.item.entity.Item
import depromeet.hotsix.obrit.item.entity.ItemReplacementHistory
import depromeet.hotsix.obrit.item.repository.ItemReplacementHistoryRepository
import depromeet.hotsix.obrit.item.repository.ItemRepository
import depromeet.hotsix.obrit.user.entity.User
import depromeet.hotsix.obrit.user.repository.UserRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

private const val ICON_PREFIX = "category-icon"
private const val FUNNEL_WINDOW_MINUTES = 5L
private val DEFAULT_FUNNEL_START_AT: LocalDateTime = LocalDateTime.of(2026, 6, 20, 12, 0)
private val DEFAULT_FUNNEL_END_AT: LocalDateTime = LocalDateTime.of(2026, 6, 20, 17, 0)

@Service
class AdminBackofficeService(
    private val userRepository: UserRepository,
    private val apiAccessLogRepository: ApiAccessLogRepository,
    private val categoryRepository: CategoryRepository,
    private val categoryIconRepository: CategoryIconRepository,
    private val itemRepository: ItemRepository,
    private val itemReplacementHistoryRepository: ItemReplacementHistoryRepository,
    private val clock: Clock,
    private val fileUploader: FileUploader,
    @Qualifier("s3PublicUrlResolver") private val publicUrlResolver: UrlResolver,
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
                    spareQuantity = item.quantity,
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

    @Transactional(readOnly = true)
    fun getSignupFunnelJourney(
        startAt: LocalDateTime?,
        endAt: LocalDateTime?,
        selectedUserId: Long?,
    ): AdminSignupFunnelView {
        val windowStart = startAt ?: DEFAULT_FUNNEL_START_AT
        val windowEnd = endAt ?: DEFAULT_FUNNEL_END_AT
        if (!windowEnd.isAfter(windowStart)) {
            throw BusinessException("종료 시각은 시작 시각보다 뒤여야 합니다.")
        }

        val cohort = userRepository.findActiveCreatedBetween(windowStart, windowEnd)
            .map { CohortUser(userId = requireNotNull(it.id), signedUpAt = requireNotNull(it.createdAt)) }
        val userIds = cohort.map { it.userId }
        val logs = if (userIds.isEmpty()) {
            emptyList()
        } else {
            apiAccessLogRepository.findByUserIdsAndOccurredAtWindow(
                userIds = userIds,
                startAt = windowStart,
                endAt = windowEnd.plusMinutes(FUNNEL_WINDOW_MINUTES),
            )
        }
        val eventsByUser = logs.groupBy { it.userId }
        val perUser = cohort.map { user ->
            val events = eventsByUser[user.userId]
                .orEmpty()
                .filter { event ->
                    !event.occurredAt.isBefore(user.signedUpAt) &&
                        event.occurredAt.isBefore(user.signedUpAt.plusMinutes(FUNNEL_WINDOW_MINUTES))
                }
                .sortedWith(compareBy<ApiAccessLog> { it.occurredAt }.thenBy { it.id ?: 0L })
            user.toFunnelUser(events)
        }

        val selectedCohortUserId = selectedUserId?.takeIf { id -> perUser.any { it.userId == id } }
            ?: perUser.firstOrNull()?.userId
        val selectedCohortUser = cohort.firstOrNull { it.userId == selectedCohortUserId }
        val selectedEvents = if (selectedCohortUser == null) {
            emptyList()
        } else {
            val events = eventsByUser[selectedCohortUser.userId]
                .orEmpty()
                .filter { event ->
                    !event.occurredAt.isBefore(selectedCohortUser.signedUpAt) &&
                        event.occurredAt.isBefore(selectedCohortUser.signedUpAt.plusMinutes(FUNNEL_WINDOW_MINUTES))
                }
                .sortedWith(compareBy<ApiAccessLog> { it.occurredAt }.thenBy { it.id ?: 0L })
            val firstItemEvent = events.firstOrNull { it.statusCode < 400 && it.isFirstItemRegistration() }
            events.mapIndexed { index, event ->
                event.toTimelineRow(index + 1, selectedCohortUser.signedUpAt, firstItemEvent)
            }
        }

        return AdminSignupFunnelView(
            startAt = windowStart,
            endAt = windowEnd,
            selectedUserId = selectedCohortUserId,
            summaryRows = buildFunnelSummary(perUser),
            userRows = perUser,
            timelineRows = selectedEvents,
            dropOffRows = buildDropOffRows(perUser),
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
        val file = requireNotNull(form.file) { "아이콘 이미지 파일은 필수입니다." }
        val key = fileUploader.upload(ICON_PREFIX, file)
        val url = publicUrlResolver.resolve(key)
        categoryIconRepository.save(CategoryIcon(name = form.name.trim(), key = key, url = url))
    }

    @Transactional
    fun updateIcon(iconId: Long, form: AdminIconForm) {
        if (form.name.isBlank()) {
            throw BusinessException("아이콘 이름은 필수입니다.")
        }
        val icon = categoryIconRepository.findById(iconId)
            .orElseThrow { ResourceNotFoundException("존재하지 않는 아이콘입니다.") }
        val file = form.file
        if (file != null && !file.isEmpty) {
            val key = fileUploader.upload(ICON_PREFIX, file)
            val url = publicUrlResolver.resolve(key)
            icon.updateForAdmin(name = form.name, key = key, url = url)
        } else {
            icon.updateForAdmin(name = form.name, key = icon.key, url = icon.url)
        }
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
                quantity = form.spareQuantity,
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
            quantity = form.spareQuantity,
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
        if (form.file == null || form.file.isEmpty) {
            throw BusinessException("아이콘 이미지 파일은 필수입니다.")
        }
    }

    private fun validateItemForm(form: AdminItemForm) {
        if (form.name.isBlank()) {
            throw BusinessException("아이템 이름은 필수입니다.")
        }
        if (form.spareQuantity < 0) {
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

    private fun CohortUser.toFunnelUser(events: List<ApiAccessLog>): AdminSignupFunnelUserRow {
        val successEvents = events.filter { it.statusCode < 400 }
        val firstItemEvent = successEvents.firstOrNull { it.isFirstItemRegistration() }
        val reachedAdd = firstItemEvent != null
        val reachedHome = successEvents.any { it.isViewHome() }
        val reachedAdditionalAction = successEvents.any { it.isAdditionalActionAfter(firstItemEvent) }
        val reachedOcr = successEvents.any { it.isOcrUsed() }
        val lastSuccessEvent = successEvents.maxWithOrNull(
            compareBy<ApiAccessLog> { it.occurredAt }.thenBy {
                it.id
                    ?: 0L
            },
        )

        return AdminSignupFunnelUserRow(
            userId = userId,
            signedUpAt = signedUpAt,
            reachedAddFirstItem = reachedAdd,
            reachedViewHome = reachedHome,
            reachedAdditionalAction = reachedAdditionalAction,
            reachedOcrUsed = reachedOcr,
            furthestStep = when {
                reachedOcr -> 5
                reachedAdditionalAction -> 4
                reachedHome -> 3
                reachedAdd -> 2
                else -> 1
            },
            eventCount = events.size,
            lastSuccessCall = lastSuccessEvent?.let { "${it.method} ${it.pathTemplate} ${it.statusCode}" } ?: "(none)",
        )
    }

    private fun ApiAccessLog.toTimelineRow(
        seq: Int,
        signedUpAt: LocalDateTime,
        firstItemEvent: ApiAccessLog?,
    ): AdminSignupFunnelTimelineRow = AdminSignupFunnelTimelineRow(
        seq = seq,
        elapsedSec = Duration.between(signedUpAt, occurredAt).seconds,
        method = method,
        pathTemplate = pathTemplate,
        statusCode = statusCode,
        durationMs = durationMs,
        signal = when {
            statusCode >= 400 -> "error"
            isSameEvent(firstItemEvent) -> "first_item"
            isAdditionalActionAfter(firstItemEvent) -> "additional_action"
            isOcrUsed() -> "ocr_used"
            isViewHome() -> "view_home"
            else -> ""
        },
    )

    private fun buildFunnelSummary(users: List<AdminSignupFunnelUserRow>): List<AdminSignupFunnelStepRow> {
        val counts = listOf(
            "1_signup" to users.size,
            "2_first_item" to users.count { it.reachedAddFirstItem },
            "3_view_home" to users.count { it.reachedViewHome },
            "4_additional_action" to users.count { it.reachedAdditionalAction },
            "5_ocr_used" to users.count { it.reachedOcrUsed },
        )

        return counts.mapIndexed { index, (step, usersCount) ->
            val previous = counts.getOrNull(index - 1)?.second
            AdminSignupFunnelStepRow(
                step = step,
                label = step.substringAfter("_"),
                users = usersCount,
                previousConversionRate = if (previous == null) {
                    "100%"
                } else {
                    formatRate(usersCount, previous)
                },
            )
        }
    }

    private fun buildDropOffRows(users: List<AdminSignupFunnelUserRow>): List<AdminSignupFunnelDropOffRow> = users
        .filter { it.furthestStep < 5 }
        .groupingBy {
            DropOffKey(
                stepReached = it.furthestStep,
                lastSuccessCall = it.lastSuccessCall,
            )
        }
        .eachCount()
        .map { (key, count) ->
            val callParts = key.lastSuccessCall.split(" ")
            AdminSignupFunnelDropOffRow(
                stepReached = key.stepReached,
                method = callParts.getOrNull(0).orEmpty(),
                pathTemplate = callParts.getOrNull(1).orEmpty(),
                statusCode = callParts.getOrNull(2).orEmpty(),
                users = count,
            )
        }
        .sortedWith(compareBy<AdminSignupFunnelDropOffRow> { it.stepReached }.thenByDescending { it.users })

    private data class CohortUser(val userId: Long, val signedUpAt: LocalDateTime)

    private data class DropOffKey(val stepReached: Int, val lastSuccessCall: String)
}

private fun formatRate(numerator: Int, denominator: Int): String = if (denominator == 0) {
    "-"
} else {
    "%.1f%%".format(numerator.toDouble() / denominator.toDouble() * 100.0)
}

private fun ApiAccessLog.isFirstItemRegistration(): Boolean =
    method == "POST" && (pathTemplate == "/items" || pathTemplate == "/items/bulk")

private fun ApiAccessLog.isViewHome(): Boolean = pathTemplate == "/home/my-summary" ||
    pathTemplate == "/home/items" ||
    pathTemplate == "/home/overall-status"

private fun ApiAccessLog.isAdditionalActionAfter(firstItemEvent: ApiAccessLog?): Boolean =
    isMaintenanceAction() || (isFirstItemRegistration() && firstItemEvent != null && isAfter(firstItemEvent))

private fun ApiAccessLog.isMaintenanceAction(): Boolean =
    (method == "POST" && pathTemplate == "/items/{itemId}/replacements") ||
        (method == "PATCH" && pathTemplate == "/items/{itemId}/spare-count")

private fun ApiAccessLog.isOcrUsed(): Boolean = method == "POST" && pathTemplate == "/receipts/analyze"

private fun ApiAccessLog.isAfter(other: ApiAccessLog): Boolean =
    occurredAt.isAfter(other.occurredAt) || (occurredAt == other.occurredAt && (id ?: 0L) > (other.id ?: 0L))

private fun ApiAccessLog.isSameEvent(other: ApiAccessLog?): Boolean =
    other != null && id == other.id && occurredAt == other.occurredAt
