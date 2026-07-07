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

    @Column(name = "error_message", length = 500)
    var errorMessage: String? = null,
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
        this.errorMessage = errorMessage
        status = ReceiptJobStatus.FAILED
    }
}
