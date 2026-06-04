package depromeet.hotsix.obrit.category.entity

import depromeet.hotsix.obrit.global.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "categories",
    indexes = [
        Index(name = "idx_categories_user_deleted", columnList = "user_id, deleted_at"),
    ],
)
class Category(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "user_id")
    var userId: Long? = null,

    @Column(nullable = false)
    var name: String = "",

    @Column(name = "icon_id", nullable = false)
    var iconId: Long = 0,

    @Column(name = "default_replacement_interval_days", nullable = false)
    var defaultReplacementIntervalDays: Int = 1,
) : BaseTimeEntity() {

    val isPreset: Boolean
        get() = userId == null

    fun isUserCategoryOf(userId: Long): Boolean = this.userId == userId

    fun updateForAdmin(name: String, iconId: Long, defaultReplacementIntervalDays: Int) {
        this.name = name.trim()
        this.iconId = iconId
        this.defaultReplacementIntervalDays = defaultReplacementIntervalDays
    }
}
