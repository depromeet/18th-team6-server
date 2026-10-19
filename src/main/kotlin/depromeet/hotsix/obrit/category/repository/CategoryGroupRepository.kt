package depromeet.hotsix.obrit.category.repository

import depromeet.hotsix.obrit.category.entity.CategoryGroup
import depromeet.hotsix.obrit.category.entity.CategoryGroupType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface CategoryGroupRepository : JpaRepository<CategoryGroup, Long> {

    @Query(
        """
        select g
        from CategoryGroup g
        where g.deletedAt is null
        order by g.displayOrder asc, g.id asc
        """,
    )
    fun findActiveOrderByDisplayOrder(): List<CategoryGroup>

    fun findFirstByTypeAndDeletedAtIsNullOrderByIdAsc(type: CategoryGroupType): CategoryGroup?
}
