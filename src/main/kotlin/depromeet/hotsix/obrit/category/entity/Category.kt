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
        Index(name = "idx_categories_group", columnList = "category_group_id"),
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

    /** 소속 카테고리(`CategoryGroup`) ID. 아직 배정되지 않은 기본 소모품은 null */
    @Column(name = "category_group_id")
    var categoryGroupId: Long? = null,
) : BaseTimeEntity() {

    val isPreset: Boolean
        get() = userId == null

    fun isUserCategoryOf(userId: Long): Boolean = this.userId == userId

    fun updateForAdmin(name: String, iconId: Long, defaultReplacementIntervalDays: Int) {
        this.name = name.trim()
        this.iconId = iconId
        this.defaultReplacementIntervalDays = defaultReplacementIntervalDays
    }

    companion object {
        /** 사용자가 직접 만든 소모품의 기본 교체 주기 */
        const val USER_DEFAULT_REPLACEMENT_INTERVAL_DAYS = 90

        /** 사용자가 직접 만든 소모품과 매칭 실패 영수증 소모품에 쓰는 공용 아이콘 */
        const val USER_DEFAULT_ICON_ID = 1L
    }
}
