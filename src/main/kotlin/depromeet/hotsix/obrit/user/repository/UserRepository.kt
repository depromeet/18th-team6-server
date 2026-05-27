package depromeet.hotsix.obrit.user.repository

import depromeet.hotsix.obrit.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {

    fun findByUuidAndDeletedAtIsNull(uuid: String): User?

    fun findAllByOrderByIdAsc(): List<User>
}
