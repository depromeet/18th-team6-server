package depromeet.hotsix.obrit.admin.dto

import java.time.LocalDate
import java.time.LocalDateTime

data class AdminUserRow(
    val id: Long,
    val uuid: String?,
    val name: String,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
    val deletedAt: LocalDateTime?,
)

data class AdminCategoryRow(
    val id: Long,
    val userId: Long?,
    val scope: String,
    val name: String,
    val iconId: Long,
    val iconUrl: String?,
    val defaultReplacementIntervalDays: Int,
    val itemCount: Int,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
    val deletedAt: LocalDateTime?,
)

data class AdminItemRow(
    val id: Long,
    val userId: Long,
    val categoryId: Long,
    val categoryName: String,
    val name: String,
    val spareQuantity: Int,
    val replacementIntervalDays: Int,
    val lastReplacedDate: LocalDate,
    val nextReplacementDate: LocalDate,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
    val deletedAt: LocalDateTime?,
)

data class AdminCategoryOption(val id: Long, val userId: Long?, val label: String)

data class AdminIconOption(val id: Long, val name: String, val url: String)

data class AdminIconRow(
    val id: Long,
    val name: String,
    val url: String,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
    val deletedAt: LocalDateTime?,
)

data class AdminUserForm(val uuid: String = "", val name: String = "")

data class AdminIconForm(val name: String = "", val url: String = "")

data class AdminCategoryForm(
    val userId: Long? = null,
    val name: String = "",
    val iconId: Long = 0,
    val defaultReplacementIntervalDays: Int = 1,
)

data class AdminItemForm(
    val userId: Long = 0,
    val categoryId: Long = 0,
    val name: String = "",
    val spareQuantity: Int = 0,
    val lastReplacedDate: LocalDate? = null,
    val replacementIntervalDays: Int = 1,
)

data class AdminReplacementForm(val replacedDate: LocalDate? = null)
