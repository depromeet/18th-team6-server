package depromeet.hotsix.obrit.receipt.client

data class ReceiptOcrClientRequest(val contents: List<ReceiptOcrContent>)

data class ReceiptOcrContent(val parts: List<ReceiptOcrPart>)

data class ReceiptOcrPart(val text: String? = null, val inlineData: ReceiptOcrInlineData? = null)

data class ReceiptOcrInlineData(val mimeType: String, val data: String)
