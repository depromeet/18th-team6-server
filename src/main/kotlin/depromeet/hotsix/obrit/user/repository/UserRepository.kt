package depromeet.hotsix.obrit.user.repository

import depromeet.hotsix.obrit.user.entity.User
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface UserRepository : JpaRepository<User, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :userId")
    fun findByIdForUpdate(@Param("userId") userId: Long): User?

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
