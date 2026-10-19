package depromeet.hotsix.obrit.receipt.client

import depromeet.hotsix.obrit.receipt.dto.BatchOcrResponse
import depromeet.hotsix.obrit.receipt.dto.OcrAnalysisResponse
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronizationManager

@Component
@Profile("test")
@Primary
class StubReceiptOcrClient : ReceiptOcrClient {

    var response: OcrAnalysisResponse = OcrAnalysisResponse()
    var error: RuntimeException? = null
    var batchResponse: BatchOcrResponse = BatchOcrResponse()
    var batchError: RuntimeException? = null

    /** 마지막 배치 호출이 트랜잭션 안에서 실행됐는지. 외부 I/O의 트랜잭션 분리를 검증할 때 쓴다. */
    var batchCalledInTransaction: Boolean? = null

    override fun analyzeImage(imageBytes: ByteArray, mimeType: String): OcrAnalysisResponse {
        error?.let { throw it }
        return response
    }

    override fun analyzeImages(images: List<BatchOcrImage>): BatchOcrResponse {
        batchCalledInTransaction = TransactionSynchronizationManager.isActualTransactionActive()
        batchError?.let { throw it }
        return batchResponse
    }

    fun reset() {
        response = OcrAnalysisResponse()
        error = null
        batchResponse = BatchOcrResponse()
        batchError = null
        batchCalledInTransaction = null
    }
}
