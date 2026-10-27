package depromeet.hotsix.obrit.receipt.service

import depromeet.hotsix.obrit.receipt.client.BatchOcrImage
import depromeet.hotsix.obrit.receipt.client.ReceiptOcrClient
import depromeet.hotsix.obrit.receipt.dto.BatchOcrResponse
import depromeet.hotsix.obrit.receipt.dto.OcrAnalysisResponse
import org.springframework.stereotype.Service

@Service
open class OcrService(private val receiptOcrClient: ReceiptOcrClient) {

    open fun analyzeReceiptImage(imageBytes: ByteArray, mimeType: String): OcrAnalysisResponse =
        receiptOcrClient.analyzeImage(imageBytes, mimeType)

    fun analyzeReceiptImages(images: List<BatchOcrImage>): BatchOcrResponse = receiptOcrClient.analyzeImages(images)
}
