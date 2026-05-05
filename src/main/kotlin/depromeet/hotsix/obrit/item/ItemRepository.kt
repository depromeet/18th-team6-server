package depromeet.hotsix.obrit.item

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ItemRepository : JpaRepository<Item, Long> {

    @Query(
        """
        select i
        from Item i
        join fetch i.category
        where i.user.id = :userId
          and i.deletedAt is null
        order by i.nextReplacementDate asc, i.id asc
        """,
    )
    fun findActiveByUserId(@Param("userId") userId: Long): List<Item>

    @Query(
        """
        select i
        from Item i
        join fetch i.category
        where i.id = :itemId
          and i.user.id = :userId
          and i.deletedAt is null
        """,
    )
    fun findActiveByIdAndUserId(@Param("itemId") itemId: Long, @Param("userId") userId: Long): Item?

    @Query(
        """
        select i
        from Item i
        where i.category.id = :categoryId
          and i.user.id = :userId
          and i.deletedAt is null
        """,
    )
    fun findActiveByCategoryIdAndUserId(
        @Param("categoryId")
        categoryId: Long,
        @Param("userId")
        userId: Long,
    ): List<Item>
}
