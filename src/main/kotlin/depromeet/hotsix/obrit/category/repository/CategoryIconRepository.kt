package depromeet.hotsix.obrit.category.repository

import depromeet.hotsix.obrit.category.entity.CategoryIcon
import org.springframework.data.jpa.repository.JpaRepository

interface CategoryIconRepository : JpaRepository<CategoryIcon, Long> {

    fun findAllByOrderByIdAsc(): List<CategoryIcon>

    fun findAllByOrderByIdDesc(): List<CategoryIcon>
}
