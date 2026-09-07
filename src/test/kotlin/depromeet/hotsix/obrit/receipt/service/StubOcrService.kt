package depromeet.hotsix.obrit.receipt.service

import depromeet.hotsix.obrit.receipt.client.ReceiptOcrClient
import depromeet.hotsix.obrit.receipt.dto.OcrAnalysisResponse
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

/**
 * 테스트에서 OCR 외부 호출을 대체한다. [behavior]를 바꿔 성공·실패를 지정한다.
 */
@Service
@Primary
@Profile("test")
class StubOcrService(receiptOcrClient: ReceiptOcrClient) : OcrService(receiptOcrClient) {

    var behavior: () -> OcrAnalysisResponse = { OcrAnalysisResponse() }

    override fun analyzeReceiptImage(imageBytes: ByteArray, mimeType: String): OcrAnalysisResponse = behavior()

    fun reset() {
        behavior = { OcrAnalysisResponse() }
    }
}
