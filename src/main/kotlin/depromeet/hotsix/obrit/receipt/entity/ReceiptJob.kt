package depromeet.hotsix.obrit.receipt.entity

import depromeet.hotsix.obrit.global.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "receipt_jobs",
    indexes = [
        Index(name = "idx_receipt_jobs_status_id", columnList = "status, id"),
    ],
)
class ReceiptJob(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "user_id", nullable = false)
    var userId: Long,

    @Column(name = "image_key", nullable = false, length = 512)
    var imageKey: String,

    @Column(name = "mime_type", nullable = false, length = 100)
    var mimeType: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ReceiptJobStatus = ReceiptJobStatus.PENDING,

    @Column(name = "result_json", columnDefinition = "TEXT")
    var resultJson: String? = null,

    @Column(name = "error_message", length = ERROR_MESSAGE_MAX_LENGTH)
    var errorMessage: String? = null,

    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0,
) : BaseTimeEntity() {

    fun markProcessing() {
        status = ReceiptJobStatus.PROCESSING
    }

    fun markPending() {
        status = ReceiptJobStatus.PENDING
    }

    fun markCompleted(resultJson: String) {
        this.resultJson = resultJson
        this.errorMessage = null
        status = ReceiptJobStatus.COMPLETED
    }

    fun markFailed(errorMessage: String) {
        this.errorMessage = errorMessage.take(ERROR_MESSAGE_MAX_LENGTH)
        status = ReceiptJobStatus.FAILED
    }

    /** 재시도 횟수를 늘리고 다시 대기중으로 되돌린다. 마지막 실패 사유를 기록한다. */
    fun retry(errorMessage: String) {
        retryCount++
        this.errorMessage = errorMessage.take(ERROR_MESSAGE_MAX_LENGTH)
        status = ReceiptJobStatus.PENDING
    }

    companion object {
        /** error_message 컬럼 길이. 초과 시 저장 자체가 실패해 잡이 멈추지 않도록 잘라서 저장한다. */
        const val ERROR_MESSAGE_MAX_LENGTH = 500
    }
}
