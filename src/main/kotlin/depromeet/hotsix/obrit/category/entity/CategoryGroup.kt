package depromeet.hotsix.obrit.category.entity

import depromeet.hotsix.obrit.global.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * 소모품(`Category`)을 묶는 카테고리. PRD의 '카테고리'(욕실, 주방 등)에 해당한다.
 */
@Entity
@Table(name = "category_groups")
class CategoryGroup(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, length = 50)
    var name: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var type: CategoryGroupType = CategoryGroupType.PRESET,

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0,
) : BaseTimeEntity()
