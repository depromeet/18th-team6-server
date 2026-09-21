package depromeet.hotsix.obrit.notification.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class NotificationDeepLinkService(
    @param:Value("\${notification.deep-link.item-template:obrit://items/{itemId}}")
    private val itemTemplate: String,
    @param:Value("\${notification.deep-link.home:obrit://home}")
    private val home: String,
) {
    fun resolve(itemId: Long?): String = itemId?.let { itemTemplate.replace("{itemId}", it.toString()) } ?: home
}
