package depromeet.hotsix.obrit.item.client

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "ocr.ai")
data class ReceiptOcrProperties(
    var apiKey: String = "",
    var url: String = "",
    var prompt: String = "",
    var authHeader: String = "",
)
