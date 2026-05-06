package depromeet.hotsix.obrit.category.repository

import depromeet.hotsix.obrit.category.entity.Category

object CategoryFixture {

    fun presetCategory(
        name: String = "기본_카테고리",
        imageUrl: String = "https://example.com/default.png",
        defaultReplacementIntervalDays: Int = 30,
    ) = Category(
        id = null,
        userId = null,
        name = name,
        imageUrl = imageUrl,
        defaultReplacementIntervalDays = defaultReplacementIntervalDays,
    )

    fun userCategory(
        userId: Long,
        name: String = "사용자_카테고리",
        imageUrl: String = "https://example.com/user.png",
        defaultReplacementIntervalDays: Int = 30,
    ) = Category(
        id = null,
        userId = userId,
        name = name,
        imageUrl = imageUrl,
        defaultReplacementIntervalDays = defaultReplacementIntervalDays,
    )

    fun presetCategories() = listOf(
        presetCategory(name = "면도기", imageUrl = "https://example.com/razor.png"),
        presetCategory(
            name = "정수기 필터",
            imageUrl = "https://example.com/filter.png",
            defaultReplacementIntervalDays = 90,
        ),
        presetCategory(
            name = "칫솔",
            imageUrl = "https://example.com/toothbrush.png",
            defaultReplacementIntervalDays = 90,
        ),
    )

    fun userCategories(userId: Long) = listOf(
        userCategory(userId = userId, name = "커스텀1", imageUrl = "https://example.com/custom1.png"),
        userCategory(
            userId = userId,
            name = "커스텀2",
            imageUrl = "https://example.com/custom2.png",
            defaultReplacementIntervalDays = 60,
        ),
    )

    fun otherUserCategory(userId: Long = 999L) = userCategory(
        userId = userId,
        name = "다른사용자카테고리",
        imageUrl = "https://example.com/other.png",
    )
}
