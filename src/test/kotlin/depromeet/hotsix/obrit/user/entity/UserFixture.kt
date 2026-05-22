package depromeet.hotsix.obrit.user.entity

object UserFixture {

    fun user(id: Long? = null, uuid: String = "550e8400-e29b-41d4-a716-446655440000", name: String = "테스트_사용자") = User(
        id = id,
        uuid = uuid,
        name = name,
    )
}
