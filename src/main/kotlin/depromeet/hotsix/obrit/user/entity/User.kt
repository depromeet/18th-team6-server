package depromeet.hotsix.obrit.user.entity

import depromeet.hotsix.obrit.global.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(length = 36)
    var uuid: String? = null,

    @Column(nullable = false)
    var name: String = "",
) : BaseTimeEntity() {

    fun updateName(name: String) {
        this.name = name.trim()
    }
}
