package depromeet.hotsix.obrit.item.client

import com.fasterxml.jackson.databind.ObjectMapper
import depromeet.hotsix.obrit.global.exception.BusinessException
import depromeet.hotsix.obrit.receipt.dto.OcrAnalysisResponse
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestClient
import java.util.Base64

@Component
class ReceiptOcrClient(
    private val receiptOcrProperties: ReceiptOcrProperties,
    private val restClient: RestClient = RestClient.create(),
) {

    private val objectMapper = ObjectMapper()

    fun analyzeImage(imageBytes: ByteArray): OcrAnalysisResponse {
        if (receiptOcrProperties.apiKey.isEmpty()) {
            throw BusinessException("OCR AI 설정이 누락되었습니다.")
        }

        val imageBase64 = Base64.getEncoder().encodeToString(imageBytes)
        val aiRequest = buildReceiptOcrRequest(imageBase64, receiptOcrProperties.prompt)

        return try {
            val response = restClient
                .post()
                .uri(receiptOcrProperties.url)
                .header("x-goog-api-key", receiptOcrProperties.apiKey)
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

    private fun buildReceiptOcrRequest(imageBase64: String, prompt: String): ReceiptOcrClientRequest =
        ReceiptOcrClientRequest(
            contents = listOf(
                ReceiptOcrContent(
                    parts = listOf(
                        ReceiptOcrPart(
                            inlineData = ReceiptOcrInlineData(
                                mimeType = "image/jpeg",
                                data = imageBase64,
                            ),
                        ),
                        ReceiptOcrPart(text = prompt),
                    ),
                ),
            ),
        )
}
