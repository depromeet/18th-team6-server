package depromeet.hotsix.obrit.receipt.client

import depromeet.hotsix.obrit.receipt.dto.BatchOcrResponse
import depromeet.hotsix.obrit.receipt.dto.OcrAnalysisResponse
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import tools.jackson.databind.ObjectMapper
import java.util.Base64

@Component
class GeminiReceiptOcrClient(
    private val receiptOcrProperties: ReceiptOcrProperties,
    private val objectMapper: ObjectMapper,
) : ReceiptOcrClient {

    private val restClient: RestClient = run {
        val factory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(10000)
            setReadTimeout(30000)
        }
        RestClient.builder()
            .requestFactory(factory)
            .build()
    }

    override fun analyzeImage(imageBytes: ByteArray, mimeType: String): OcrAnalysisResponse {
        val imageBase64 = Base64.getEncoder().encodeToString(imageBytes)
        val request = buildSingleRequest(imageBase64, receiptOcrProperties.prompt, mimeType)
        return callGemini(request) { objectMapper.readValue(it, OcrAnalysisResponse::class.java) }
    }

    override fun analyzeImages(images: List<BatchOcrImage>): BatchOcrResponse {
        val request = buildBatchRequest(images, receiptOcrProperties.prompt)
        return callGemini(request) { objectMapper.readValue(it, BatchOcrResponse::class.java) }
    }

    private fun <T> callGemini(request: ReceiptOcrClientRequest, parse: (String) -> T): T = try {
        val response = restClient
            .post()
            .uri(receiptOcrProperties.url)
            .header(receiptOcrProperties.authHeader, receiptOcrProperties.apiKey)
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body(ReceiptOcrClientResponse::class.java)
            ?: throw OcrFailedException(OcrFailureReason.EMPTY_RESPONSE, "API 응답이 비어있습니다.")

        val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw OcrFailedException(OcrFailureReason.EMPTY_RESPONSE, "API 응답에서 분석 결과를 찾을 수 없습니다.")

        parse(jsonText)
    } catch (e: HttpServerErrorException) {
        // 5xx 서버 에러 (503 등)
        throw OcrFailedException(
            OcrFailureReason.UPSTREAM_5XX,
            "AI API 서버 오류 (${e.statusCode}): 잠시 후 다시 시도해주세요.",
        )
    } catch (e: ResourceAccessException) {
        // 연결·읽기 타임아웃, 네트워크 단절
        throw OcrFailedException(OcrFailureReason.TIMEOUT, "AI API 응답 시간이 초과되었습니다. 잠시 후 다시 시도해주세요.")
    } catch (e: OcrFailedException) {
        throw e
    } catch (e: Exception) {
        // 4xx, 응답 파싱 실패 등
        val message = when {
            "503" in (e.message ?: "") -> "AI API 일시적 오류: 잠시 후 다시 시도해주세요."
            "Service Unavailable" in (e.message ?: "") -> "AI API 일시적 오류: 잠시 후 다시 시도해주세요."
            else -> "AI API 호출 실패: ${e.message}"
        }
        throw OcrFailedException(OcrFailureReason.UNKNOWN, message)
    }

    private fun buildSingleRequest(imageBase64: String, prompt: String, mimeType: String): ReceiptOcrClientRequest =
        ReceiptOcrClientRequest(
            contents = listOf(
                ReceiptOcrContent(
                    parts = listOf(
                        ReceiptOcrPart(inlineData = ReceiptOcrInlineData(mimeType = mimeType, data = imageBase64)),
                        ReceiptOcrPart(text = prompt),
                    ),
                ),
            ),
        )

    private fun buildBatchRequest(images: List<BatchOcrImage>, prompt: String): ReceiptOcrClientRequest {
        val parts = buildList {
            images.forEach { image ->
                add(ReceiptOcrPart(text = "receipt_id: ${image.receiptId}"))
                add(
                    ReceiptOcrPart(
                        inlineData = ReceiptOcrInlineData(
                            mimeType = image.mimeType,
                            data = Base64.getEncoder().encodeToString(image.bytes),
                        ),
                    ),
                )
            }
            add(ReceiptOcrPart(text = prompt + BATCH_INSTRUCTION))
        }
        return ReceiptOcrClientRequest(contents = listOf(ReceiptOcrContent(parts = parts)))
    }

    companion object {
        private val BATCH_INSTRUCTION = """

            여러 영수증이 제공된다. 각 영수증은 바로 앞의 "receipt_id: <ID>" 텍스트로 식별된다.
            각 영수증을 위 규칙대로 분석하고, 설명이나 마크다운 코드블록 없이 아래 JSON만 출력한다.
            {"results":[{"receipt_id":"<ID>","store":"매장명","date":"YYYY-MM-DD 또는 null","items":[...위 items 스키마...],"total":합계}]}
        """.trimIndent()
    }
}
