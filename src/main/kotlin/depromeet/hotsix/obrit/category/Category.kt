package depromeet.hotsix.obrit.category

import depromeet.hotsix.obrit.common.BaseTimeEntity
import depromeet.hotsix.obrit.user.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: User? = null,

    @Column(nullable = false)
    var name: String = "",

    @Column(name = "image_url", nullable = false)
    var imageUrl: String = "",

    @Column(name = "default_replacement_interval_days", nullable = false)
    var defaultReplacementIntervalDays: Int = 1,
) : BaseTimeEntity() {

    val isPreset: Boolean
        get() = user == null
}
