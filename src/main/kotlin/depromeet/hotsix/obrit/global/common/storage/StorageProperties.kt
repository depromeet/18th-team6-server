package depromeet.hotsix.obrit.global.common.storage

import org.springframework.boot.context.properties.ConfigurationProperties
import java.nio.file.Path

// rootDir: 업로드 파일을 저장하는 디렉터리, publicBaseUrl: 클라이언트가 접근하는 앱의 외부 주소
@ConfigurationProperties(prefix = "storage")
data class StorageProperties(val rootDir: Path, val publicBaseUrl: String) {

    companion object {
        const val PUBLIC_PATH = "/files"

        // 외부에 공개하는 prefix. 영수증(receipts)은 개인정보가 있어 공개하지 않는다.
        const val PUBLIC_ICON_PREFIX = "category-icon"
    }
}
