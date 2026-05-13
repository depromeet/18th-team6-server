package depromeet.hotsix.obrit.home.entity

import depromeet.hotsix.obrit.item.entity.ItemStatus

// 전체 종합 상태
enum class OverallStatus {
    PERFECT,
    GOOD,
    WARNING,
    DANGER,
    ;

    companion object {
        fun of(replacement: ItemStatus, spare: ItemStatus): OverallStatus = when {
            replacement == ItemStatus.GOOD && spare == ItemStatus.GOOD -> PERFECT
            replacement == ItemStatus.GOOD && spare == ItemStatus.WARNING -> GOOD
            replacement == ItemStatus.WARNING && spare == ItemStatus.GOOD -> GOOD
            replacement == ItemStatus.GOOD && spare == ItemStatus.DANGER -> WARNING
            replacement == ItemStatus.DANGER && spare == ItemStatus.GOOD -> WARNING
            replacement == ItemStatus.WARNING && spare == ItemStatus.WARNING -> WARNING
            else -> DANGER
        }
    }
}
