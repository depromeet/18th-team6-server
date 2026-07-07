package depromeet.hotsix.obrit.receipt.client

import depromeet.hotsix.obrit.receipt.dto.OcrAnalysisResponse
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("test")
@Primary
class StubReceiptOcrClient : ReceiptOcrClient {

    var response: OcrAnalysisResponse = OcrAnalysisResponse()
    var error: RuntimeException? = null

    override fun analyzeImage(imageBytes: ByteArray, mimeType: String): OcrAnalysisResponse {
        error?.let { throw it }
        return response
    }

    fun reset() {
        response = OcrAnalysisResponse()
        error = null
    }
}
