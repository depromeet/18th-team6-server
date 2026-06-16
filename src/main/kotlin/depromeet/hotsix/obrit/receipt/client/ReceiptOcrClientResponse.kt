package depromeet.hotsix.obrit.receipt.client

data class ReceiptOcrClientResponse(val candidates: List<ReceiptOcrCandidate>?)

data class ReceiptOcrCandidate(val content: ReceiptOcrContent?)
