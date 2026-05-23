package depromeet.hotsix.obrit.category.entity

import depromeet.hotsix.obrit.global.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "icons")
class CategoryIcon(
    @Column(nullable = false)
    var name: String,

    @Column(nullable = false)
    var url: String,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
) : BaseTimeEntity() {

    fun updateForAdmin(name: String, url: String) {
        this.name = name.trim()
        this.url = url.trim()
    }
}
