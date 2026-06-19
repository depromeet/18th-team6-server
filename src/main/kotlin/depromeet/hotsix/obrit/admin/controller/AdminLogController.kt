package depromeet.hotsix.obrit.admin.controller

import depromeet.hotsix.obrit.admin.dto.LogFileResponse
import depromeet.hotsix.obrit.admin.service.LogTailService
import depromeet.hotsix.obrit.global.dto.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Admin Logs", description = "어드민 로그 조회 API")
@RestController
@RequestMapping("/admin/logs")
class AdminLogController(private val logTailService: LogTailService) {

    // 로그 내용이 아니라 logs/ 폴더에 어떤 로그 파일들이 있는지 목록만 반환한다.
    @Operation(summary = "로그 파일 목록 조회", description = "logs/ 디렉토리의 로그 파일 목록을 최신순으로 반환합니다.")
    @GetMapping("/files")
    fun files(): ApiResponse<List<LogFileResponse>> = ApiResponse.ok(logTailService.listFiles())

    @Operation(summary = "로그 파일 tail", description = "지정 파일의 마지막 N줄을 text/plain으로 반환합니다.")
    @GetMapping("/tail", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun tail(
        @Parameter(description = "조회할 파일명 (예: obrit.log)", required = true)
        @RequestParam file: String,
        @Parameter(description = "조회할 줄 수 (1~1000, 기본 200)")
        @RequestParam(defaultValue = "200") lines: Int,
    ): String = logTailService.tail(file, lines)
}
