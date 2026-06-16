package depromeet.hotsix.obrit.receipt.client

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "ocr.client")
data class ReceiptOcrProperties(
    val apiKey: String = "",
    val url: String = "",
    val prompt: String = "",
    val authHeader: String = "",
)
