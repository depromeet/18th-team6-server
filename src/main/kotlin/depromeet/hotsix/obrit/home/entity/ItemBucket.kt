package depromeet.hotsix.obrit.home.entity

import depromeet.hotsix.obrit.item.entity.ItemStatus

// 아이템 분류 버킷 6종류
enum class ItemBucket(val priority: Int, val status: ItemStatus) {
    NONE_OVERDUE(1, ItemStatus.DANGER),
    NONE_WARN(2, ItemStatus.DANGER),
    HAS_OVERDUE(3, ItemStatus.DANGER),
    HAS_WARN(4, ItemStatus.WARNING),
    NONE_SAFE(5, ItemStatus.WARNING),
    HAS_SAFE(6, ItemStatus.GOOD),
}
