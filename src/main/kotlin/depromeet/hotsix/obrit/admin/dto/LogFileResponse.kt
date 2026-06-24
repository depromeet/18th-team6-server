package depromeet.hotsix.obrit.admin.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "로그 파일 정보")
data class LogFileResponse(
    @field:Schema(description = "파일명", example = "obrit.log")
    val name: String,
    @field:Schema(description = "파일 크기 (바이트)", example = "1234567")
    val sizeBytes: Long,
    @field:Schema(description = "마지막 수정 시각 (ISO-8601 LocalDateTime)", example = "2026-06-19T03:15:42")
    val lastModified: LocalDateTime,
)
