package depromeet.hotsix.obrit.item.entity

// 여분 유무
enum class SpareBand {
    NONE,
    HAS,
    ;

    companion object {
        fun of(quantity: Int): SpareBand = if (quantity == 0) NONE else HAS
    }
}
