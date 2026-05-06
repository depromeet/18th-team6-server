package depromeet.hotsix.obrit.category.repository

import depromeet.hotsix.obrit.category.entity.Category
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CategoryRepository : JpaRepository<Category, Long> {

    @Query(
        """
        select c
        from Category c
        where c.userId = :userId
          and c.id > :cursor
          and c.deletedAt is null
        """,
    )
    fun findByUserIdWithCursor(
        @Param("userId") userId: Long,
        @Param("cursor") cursor: Long,
        pageable: Pageable,
    ): Slice<Category>

    @Query(
        """
        select c
        from Category c
        where c.userId is null
          and c.id > :cursor
          and c.deletedAt is null
        """,
    )
    fun findByUserIdIsNullWithCursor(@Param("cursor") cursor: Long, pageable: Pageable): Slice<Category>

    fun findByUserIdIsNullAndDeletedAtIsNull(pageable: Pageable): Slice<Category>

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
        @Param("userId") userId: Long,
        @Param("categoryIds") categoryIds: Collection<Long>,
    ): List<Category>
}
