package depromeet.hotsix.obrit.item.repository

import depromeet.hotsix.obrit.item.entity.Item
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ItemRepository :
    JpaRepository<Item, Long>,
    ItemQueryRepository {

    fun findAllByOrderByIdAsc(): List<Item>

    fun findAllByDeletedAtIsNull(): List<Item>

    @Query(
        """
        select i
        from Item i
        where i.userId = :userId
          and i.deletedAt is null
        order by i.nextReplacementDate asc, i.id asc
        """,
    )
    fun findActiveByUserId(@Param("userId") userId: Long): List<Item>

    @Query(
        """
        select i
        from Item i
        where i.id = :itemId
          and i.userId = :userId
          and i.deletedAt is null
        """,
    )
    fun findActiveByIdAndUserId(@Param("itemId") itemId: Long, @Param("userId") userId: Long): Item?

    @Query(
        """
        select i
        from Item i
        where i.categoryId = :categoryId
          and i.userId = :userId
          and i.deletedAt is null
        """,
    )
    fun findActiveByCategoryIdAndUserId(
        @Param("categoryId")
        categoryId: Long,
        @Param("userId")
        userId: Long,
    ): List<Item>

    fun findAllByUserId(userId: Long): List<Item>

    fun findAllByCategoryId(categoryId: Long): List<Item>

    fun existsByUserIdAndNameAndDeletedAtIsNull(userId: Long, name: String): Boolean

    @Query(
        """
        select i.name
        from Item i
        where i.userId = :userId
          and i.name in :names
          and i.deletedAt is null
        """,
    )
    fun findExistingNamesByUserIdAndNames(
        @Param("userId") userId: Long,
        @Param("names") names: Collection<String>,
    ): List<String>

    @Query(
        """
        select i.categoryId, count(i), coalesce(sum(i.quantity), 0)
        from Item i
        where i.userId = :userId
          and i.categoryId in :categoryIds
          and i.deletedAt is null
        group by i.categoryId
        """,
    )
    fun countByCategoryIdsAndUserId(
        @Param("categoryIds") categoryIds: Collection<Long>,
        @Param("userId") userId: Long,
    ): List<Array<Any>>
}
