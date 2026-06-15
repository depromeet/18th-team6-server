package depromeet.hotsix.obrit.category.repository

import depromeet.hotsix.obrit.category.entity.Category
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CategoryRepository : JpaRepository<Category, Long> {

    fun findAllByOrderByIdAsc(): List<Category>

    @Query(
        """
        select c
        from Category c
        where c.userId = :userId
          and c.deletedAt is null
        """,
    )
    fun findActiveByUserId(@Param("userId") userId: Long): List<Category>

    @Query(
        """
        select c
        from Category c
        where c.userId is null
          and c.deletedAt is null
        order by c.id asc
        """,
    )
    fun findActivePresets(): List<Category>

    @Query(
        """
        select c
        from Category c
        where c.id = :id
          and c.deletedAt is null
        """,
    )
    fun findActiveById(@Param("id") id: Long): Category?

    @Query(
        """
        select c
        from Category c
        where c.id = :categoryId
          and c.deletedAt is null
          and (c.userId is null or c.userId = :userId)
        """,
    )
    fun findVisibleById(@Param("userId") userId: Long, @Param("categoryId") categoryId: Long): Category?

    @Query(
        """
        select c
        from Category c
        where c.id in :categoryIds
          and c.deletedAt is null
          and (c.userId is null or c.userId = :userId)
        """,
    )
    fun findVisibleByIds(
        @Param("userId") userId: Long,
        @Param("categoryIds") categoryIds: Collection<Long>,
    ): List<Category>

    fun existsByUserIdAndNameAndDeletedAtIsNull(userId: Long, name: String): Boolean

    fun existsByUserIdIsNullAndNameAndDeletedAtIsNull(name: String): Boolean

    fun findAllByUserId(userId: Long): List<Category>

    @Query(
        """
        select c
        from Category c
        where c.userId = :userId
          and c.name = :name
          and c.deletedAt is null
        """,
    )
    fun findActiveByUserIdAndName(@Param("userId") userId: Long, @Param("name") name: String): Category?
}
