package depromeet.hotsix.obrit.receipt.service

import depromeet.hotsix.obrit.receipt.client.ReceiptOcrClient
import depromeet.hotsix.obrit.receipt.dto.OcrAnalysisResponse
import org.springframework.stereotype.Service

@Service
class OcrService(private val receiptOcrClient: ReceiptOcrClient) {

    fun analyzeReceiptImage(imageBytes: ByteArray, mimeType: String): OcrAnalysisResponse =
        receiptOcrClient.analyzeImage(imageBytes, mimeType)
}
