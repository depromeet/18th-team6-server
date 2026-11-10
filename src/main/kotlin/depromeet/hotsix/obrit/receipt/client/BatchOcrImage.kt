package depromeet.hotsix.obrit.receipt.client

/** 배치 OCR 호출에 넘길 이미지 하나. receiptId로 응답의 각 결과와 매핑한다. */
data class BatchOcrImage(val receiptId: String, val bytes: ByteArray, val mimeType: String)
