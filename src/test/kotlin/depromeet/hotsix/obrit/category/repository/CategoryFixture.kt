package depromeet.hotsix.obrit.category.repository

import depromeet.hotsix.obrit.category.entity.Category

object CategoryFixture {

    fun presetCategory(name: String = "기본_카테고리", iconId: Long = 1, defaultReplacementIntervalDays: Int = 30) = Category(
        id = null,
        userId = null,
        name = name,
        iconId = iconId,
        defaultReplacementIntervalDays = defaultReplacementIntervalDays,
    )

    fun userCategory(
        userId: Long,
        name: String = "사용자_카테고리",
        iconId: Long = 1,
        defaultReplacementIntervalDays: Int = 30,
    ) = Category(
        id = null,
        userId = userId,
        name = name,
        iconId = iconId,
        defaultReplacementIntervalDays = defaultReplacementIntervalDays,
    )

    fun presetCategories() = listOf(
        presetCategory(name = "면도기", iconId = 1),
        presetCategory(name = "정수기 필터", iconId = 2, defaultReplacementIntervalDays = 90),
        presetCategory(name = "칫솔", iconId = 3, defaultReplacementIntervalDays = 90),
    )

    fun userCategories(userId: Long) = listOf(
        userCategory(userId = userId, name = "커스텀1", iconId = 1),
        userCategory(userId = userId, name = "커스텀2", iconId = 2, defaultReplacementIntervalDays = 60),
    )

    fun otherUserCategory(userId: Long = 999L) = userCategory(
        userId = userId,
        name = "다른사용자카테고리",
        iconId = 1,
    )
}
