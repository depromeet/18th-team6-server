package depromeet.hotsix.obrit.global.common.storage

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.web.multipart.MultipartFile
import java.util.concurrent.ConcurrentHashMap

@Component
@Profile("test")
class StubFileUploader : FileUploader {

    private val store = ConcurrentHashMap<String, ByteArray>()

    override fun upload(prefix: String, file: MultipartFile): String {
        val key = "$prefix/${file.originalFilename}"
        store[key] = file.bytes
        return key
    }

    /** 마지막 다운로드가 트랜잭션 안에서 실행됐는지. 외부 I/O의 트랜잭션 분리를 검증할 때 쓴다. */
    var downloadCalledInTransaction: Boolean? = null

    override fun download(key: String): ByteArray {
        downloadCalledInTransaction = TransactionSynchronizationManager.isActualTransactionActive()
        return store[key] ?: throw NoSuchElementException("스텁 스토리지에 파일이 없습니다: $key")
    }
}
