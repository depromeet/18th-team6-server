package depromeet.hotsix.obrit.item.client

data class ReceiptOcrClientResponse(val candidates: List<ReceiptOcrCandidate>?)

data class ReceiptOcrCandidate(val content: ReceiptOcrContent?)
