package depromeet.hotsix.obrit.user.entity

object UserFixture {

    fun user(id: Long? = null, name: String = "테스트_사용자") = User(
        id = id,
        name = name,
    )
}
