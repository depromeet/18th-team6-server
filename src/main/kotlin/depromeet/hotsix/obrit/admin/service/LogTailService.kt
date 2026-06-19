package depromeet.hotsix.obrit.admin.service

import depromeet.hotsix.obrit.admin.dto.LogFileResponse
import depromeet.hotsix.obrit.global.exception.BusinessException
import depromeet.hotsix.obrit.global.exception.LogTailTimeoutException
import depromeet.hotsix.obrit.global.exception.ResourceNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

@Service
class LogTailService(private val logsDir: Path = Paths.get("logs")) { // 운영/로컬에서 기본 로그 파일 경로 /logs

    private val log = LoggerFactory.getLogger(LogTailService::class.java)

    // 존재하는 로그 파일명을 리스트로 가져오는 메서드
    fun listFiles(): List<LogFileResponse> {
        if (!Files.isDirectory(logsDir)) return emptyList()
        return Files.list(logsDir).use { stream ->
            stream
                .filter { Files.isRegularFile(it) } // 일반 파일만! (만약 하위 디렉토리가 있으면 제외)
                .filter { FILE_NAME_REGEX.matches(it.fileName.toString()) } // 정규식 매칭된 파일만 -> 정규식은 아래쪽에 존재
                .map { // 각 파일을 DTO로 변환
                    LogFileResponse(
                        name = it.fileName.toString(),
                        sizeBytes = Files.size(it),
                        lastModified = LocalDateTime.ofInstant(
                            Files.getLastModifiedTime(it).toInstant(),
                            ZoneId.systemDefault(),
                        ),
                    )
                }
                .sorted(compareByDescending { it.lastModified })
                .toList()
        }
    }

    // 현재 로그 파일의 기록을 가져오는 메서드 => (검증 -> 읽기 -> 결과 가져오기)
    fun tail(file: String, lines: Int): String {
        // 1. 입력값 검증
        if (file.isBlank()) throw BusinessException("file은 비어 있을 수 없습니다.")
        if (!FILE_NAME_REGEX.matches(file)) throw BusinessException("허용되지 않은 파일명입니다.")
        if (lines !in 1..MAX_LINES) throw BusinessException("lines는 1 이상 $MAX_LINES 이하여야 합니다.")

        // 2. path 검증
        val canonicalLogsDir = logsDir.toFile().canonicalFile.toPath()
        val target = logsDir.resolve(file).toFile().canonicalFile.toPath()
        if (!target.startsWith(canonicalLogsDir)) {
            throw BusinessException("허용되지 않은 경로입니다.")
        }
        if (!Files.isRegularFile(target)) {
            throw ResourceNotFoundException("로그 파일을 찾을 수 없습니다.")
        }

        // 3. 파일 읽기 + 누가 어떤 파일 읽었는지 감사 로그 => tail 사용
        log.info("admin log tail 호출: file={}, lines={}", file, lines)

        val command = if (file.endsWith(".gz")) {
            listOf("bash", "-c", "gunzip -c ${target.toAbsolutePath()} | tail -n $lines")
        } else {
            listOf("tail", "-n", lines.toString(), target.toAbsolutePath().toString())
        }

        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()

        // 4. 결과 수집
        val finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        if (!finished) {
            process.destroyForcibly()
            throw LogTailTimeoutException("로그 조회가 ${TIMEOUT_SECONDS}초 안에 완료되지 않았습니다.")
        }

        val output = process.inputStream.bufferedReader().use { it.readText() }
        if (process.exitValue() != 0) {
            throw IllegalStateException("tail 실행 실패 (exit=${process.exitValue()}): $output")
        }
        return output
    }

    // 정규식 화이트리스트
    companion object {
        private val FILE_NAME_REGEX = Regex("""^obrit(\.\d{4}-\d{2}-\d{2}\.\d+)?\.log(\.gz)?$""")
        private const val MAX_LINES = 1000
        private const val TIMEOUT_SECONDS = 2L
    }
}
