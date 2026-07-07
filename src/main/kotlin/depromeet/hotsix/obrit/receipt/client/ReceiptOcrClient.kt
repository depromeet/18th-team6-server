package depromeet.hotsix.obrit.receipt.client

import depromeet.hotsix.obrit.receipt.dto.OcrAnalysisResponse

interface ReceiptOcrClient {

    fun analyzeImage(imageBytes: ByteArray, mimeType: String): OcrAnalysisResponse
}
