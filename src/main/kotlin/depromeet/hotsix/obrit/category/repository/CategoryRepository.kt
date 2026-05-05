package depromeet.hotsix.obrit.category.repository

import depromeet.hotsix.obrit.category.entity.Category
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CategoryRepository : JpaRepository<Category, Long> {

    @Query(
        """
        select c
        from Category c
        where c.deletedAt is null
          and (c.userId is null or c.userId = :userId)
        order by case when c.userId is null then 0 else 1 end asc, c.id asc
        """,
    )
    fun findVisibleCategories(@Param("userId") userId: Long): List<Category>

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
        where c.id in :categoryIds
          and c.deletedAt is null
          and (c.userId is null or c.userId = :userId)
        """,
    )
    fun findVisibleByIds(
        @Param("userId")
        userId: Long,
        @Param("categoryIds")
        categoryIds: Collection<Long>,
    ): List<Category>
}
