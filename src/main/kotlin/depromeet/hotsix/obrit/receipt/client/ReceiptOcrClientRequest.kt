package depromeet.hotsix.obrit.receipt.client

data class ReceiptOcrClientRequest(
    val contents: List<ReceiptOcrContent>,
    val generationConfig: ReceiptOcrGenerationConfig = ReceiptOcrGenerationConfig(),
)

/** 응답을 JSON으로 강제해 코드블록·설명문이 섞여 파싱이 실패하는 것을 막는다. */
data class ReceiptOcrGenerationConfig(val responseMimeType: String = "application/json")

data class ReceiptOcrContent(val parts: List<ReceiptOcrPart>)

data class ReceiptOcrPart(val text: String? = null, val inlineData: ReceiptOcrInlineData? = null)

data class ReceiptOcrInlineData(val mimeType: String, val data: String)
