package depromeet.hotsix.obrit.category

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CategoryRepository : JpaRepository<Category, Long> {

    @Query(
        """
        select c
        from Category c
        left join fetch c.user
        where c.deletedAt is null
          and (c.user is null or c.user.id = :userId)
        order by case when c.user is null then 0 else 1 end asc, c.id asc
        """,
    )
    fun findVisibleCategories(@Param("userId") userId: Long): List<Category>

    @Query(
        """
        select c
        from Category c
        left join fetch c.user
        where c.id = :id
          and c.deletedAt is null
        """,
    )
    fun findActiveById(@Param("id") id: Long): Category?
}
