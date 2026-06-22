package depromeet.hotsix.obrit.user.repository

import depromeet.hotsix.obrit.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface UserRepository : JpaRepository<User, Long> {

    fun findByUuidAndDeletedAtIsNull(uuid: String): User?

    fun findAllByOrderByIdAsc(): List<User>

    @Query(
        """
        select u
        from User u
        where u.createdAt >= :startAt
          and u.createdAt < :endAt
          and u.deletedAt is null
        order by u.createdAt asc, u.id asc
        """,
    )
    fun findActiveCreatedBetween(
        @Param("startAt") startAt: LocalDateTime,
        @Param("endAt") endAt: LocalDateTime,
    ): List<User>
}
