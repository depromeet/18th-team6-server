package depromeet.hotsix.obrit.receipt.client

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(ReceiptOcrProperties::class)
class ReceiptOcrConfig
