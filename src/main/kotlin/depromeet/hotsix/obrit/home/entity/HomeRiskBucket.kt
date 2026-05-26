package depromeet.hotsix.obrit.home.entity

import depromeet.hotsix.obrit.item.entity.ItemStatus

// 홈 화면 위험도 버킷 (위험/경고 2종)
enum class HomeRiskBucket(val status: ItemStatus) {
    DANGER(ItemStatus.DANGER),
    WARNING(ItemStatus.WARNING),
    ;

    companion object {
        fun from(status: ItemStatus): HomeRiskBucket? = when (status) {
            ItemStatus.DANGER -> DANGER
            ItemStatus.WARNING -> WARNING
            ItemStatus.GOOD -> null
        }
    }
}
