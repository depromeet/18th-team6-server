package depromeet.hotsix.obrit.common

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import java.time.LocalDateTime

@MappedSuperclass
abstract class BaseTimeEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null
        protected set

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime? = null
        protected set

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null
        protected set

    @PrePersist
    fun prePersist() {
        val now = LocalDateTime.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = LocalDateTime.now()
    }

    fun softDelete() {
        deletedAt = LocalDateTime.now()
    }
}
