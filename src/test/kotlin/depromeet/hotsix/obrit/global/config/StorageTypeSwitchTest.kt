package depromeet.hotsix.obrit.global.config

import depromeet.hotsix.obrit.global.common.storage.FileSystemFileUploader
import depromeet.hotsix.obrit.global.common.storage.FileSystemPublicUrlResolver
import depromeet.hotsix.obrit.global.common.storage.FileUploader
import depromeet.hotsix.obrit.global.common.storage.S3PresignedUrlResolver
import depromeet.hotsix.obrit.global.common.storage.S3PublicUrlResolver
import depromeet.hotsix.obrit.global.common.storage.S3Uploader
import depromeet.hotsix.obrit.global.common.storage.UrlResolver
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner

// storage.type 값에 따라 prod에서 한 저장소 구현만 등록되는지 확인한다.
class StorageTypeSwitchTest {

    private val contextRunner = ApplicationContextRunner()
        .withPropertyValues(
            "spring.profiles.active=prod",
            "storage.root-dir=build/tmp/storage",
            "storage.public-base-url=https://api.example.com",
            "cloud.aws.s3.bucket=bucket",
            "cloud.aws.s3.region=ap-northeast-2",
        )
        .withUserConfiguration(
            StorageConfig::class.java,
            FileSystemFileUploader::class.java,
            FileSystemPublicUrlResolver::class.java,
            S3Config::class.java,
            S3Uploader::class.java,
            S3PublicUrlResolver::class.java,
            S3PresignedUrlResolver::class.java,
        )

    @Test
    fun `storage type이 없으면 파일시스템 저장소를 쓴다`() {
        contextRunner.run { context ->
            assertInstanceOf(FileSystemFileUploader::class.java, context.getBean(FileUploader::class.java))
            assertInstanceOf(
                FileSystemPublicUrlResolver::class.java,
                context.getBean("s3PublicUrlResolver", UrlResolver::class.java),
            )
        }
    }

    @Test
    fun `storage type이 s3이면 기존 S3 저장소를 쓴다`() {
        contextRunner.withPropertyValues("storage.type=s3").run { context ->
            assertInstanceOf(S3Uploader::class.java, context.getBean(FileUploader::class.java))
            assertInstanceOf(
                S3PublicUrlResolver::class.java,
                context.getBean("s3PublicUrlResolver", UrlResolver::class.java),
            )
        }
    }
}
