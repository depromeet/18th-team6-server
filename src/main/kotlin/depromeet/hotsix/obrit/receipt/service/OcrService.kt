package depromeet.hotsix.obrit.receipt.service

import depromeet.hotsix.obrit.item.client.ReceiptOcrClient
import depromeet.hotsix.obrit.receipt.dto.OcrAnalysisResponse
import org.springframework.stereotype.Service

@Service
class OcrService(private val receiptOcrClient: ReceiptOcrClient) {

    fun analyzeReceiptImage(imageBytes: ByteArray): OcrAnalysisResponse = receiptOcrClient.analyzeImage(imageBytes)
}
