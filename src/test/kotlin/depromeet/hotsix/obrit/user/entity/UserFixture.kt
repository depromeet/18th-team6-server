package depromeet.hotsix.obrit.user.entity

import java.util.UUID

object UserFixture {

    fun user(id: Long? = null, uuid: String = UUID.randomUUID().toString(), name: String = "테스트_사용자") = User(
        id = id,
        uuid = uuid,
        name = name,
    )
}
