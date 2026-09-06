package depromeet.hotsix.obrit.item.entity

// 여분 유무
enum class SpareBand {
    NONE,
    HAS,
    ;

    companion object {
        /**
         * 미입력(null)은 [HAS]로 본다. 여분이 없다고 단정할 근거가 없기 때문이다.
         * 미입력을 [NONE]으로 두면 온보딩만 마친 사용자가 전부 여분 없음으로 표시된다.
         */
        fun of(quantity: Int?): SpareBand = if (quantity == 0) NONE else HAS
    }
}
