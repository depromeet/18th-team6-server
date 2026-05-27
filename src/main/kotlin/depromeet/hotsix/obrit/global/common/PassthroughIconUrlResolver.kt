package depromeet.hotsix.obrit.global.common

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("!prod")
class PassthroughIconUrlResolver : IconUrlResolver {
    override fun resolve(key: String): String = key
}
