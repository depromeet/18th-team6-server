package depromeet.hotsix.obrit.receipt.client

import depromeet.hotsix.obrit.global.exception.BusinessException
import depromeet.hotsix.obrit.receipt.dto.OcrAnalysisResponse
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestClient
import tools.jackson.databind.ObjectMapper
import java.util.Base64

@Component
class ReceiptOcrClient(
    private val receiptOcrProperties: ReceiptOcrProperties,
    private val objectMapper: ObjectMapper,
) {

    private val restClient: RestClient = run {
        val factory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(10000)
            setReadTimeout(30000)
        }
        RestClient.builder()
            .requestFactory(factory)
            .build()
    }

    fun analyzeImage(imageBytes: ByteArray, mimeType: String = "image/jpeg"): OcrAnalysisResponse {
        if (receiptOcrProperties.apiKey.isBlank()) {
            throw BusinessException("OCR AI 설정이 누락되었습니다.")
        }

        val imageBase64 = Base64.getEncoder().encodeToString(imageBytes)
        val aiRequest = buildReceiptOcrRequest(imageBase64, receiptOcrProperties.prompt, mimeType)

        return try {
            val response = restClient
                .post()
                .uri(receiptOcrProperties.url)
                .header(receiptOcrProperties.authHeader, receiptOcrProperties.apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(aiRequest)
                .retrieve()
                .body(ReceiptOcrClientResponse::class.java)
                ?: throw BusinessException("API 응답이 비어있습니다.")

            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw BusinessException("API 응답에서 분석 결과를 찾을 수 없습니다.")

            objectMapper.readValue(jsonText, OcrAnalysisResponse::class.java)
        } catch (e: HttpServerErrorException) {
            // 5xx 서버 에러 (503 등)
            throw BusinessException(
                "AI API 서버 오류 (${e.statusCode}): 잠시 후 다시 시도해주세요.",
            )
        } catch (e: BusinessException) {
            throw e
        } catch (e: Exception) {
            // 4xx, 네트워크 오류 등
            val message = when {
                "503" in (e.message ?: "") -> "AI API 일시적 오류: 잠시 후 다시 시도해주세요."
                "Service Unavailable" in (e.message ?: "") -> "AI API 일시적 오류: 잠시 후 다시 시도해주세요."
                else -> "AI API 호출 실패: ${e.message}"
            }
            throw BusinessException(message)
        }
    }

    private fun buildReceiptOcrRequest(imageBase64: String, prompt: String, mimeType: String): ReceiptOcrClientRequest =
        ReceiptOcrClientRequest(
            contents = listOf(
                ReceiptOcrContent(
                    parts = listOf(
                        ReceiptOcrPart(
                            inlineData = ReceiptOcrInlineData(
                                mimeType = mimeType,
                                data = imageBase64,
                            ),
                        ),
                        ReceiptOcrPart(text = prompt),
                    ),
                ),
            ),
        )
}
