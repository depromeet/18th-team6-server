package depromeet.hotsix.obrit.receipt.client

import depromeet.hotsix.obrit.global.exception.BusinessException
import depromeet.hotsix.obrit.receipt.dto.BatchOcrResponse
import depromeet.hotsix.obrit.receipt.dto.OcrAnalysisResponse
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
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
        return objectMapper.readValue(callGemini(request), OcrAnalysisResponse::class.java)
    }

    override fun analyzeImages(images: List<BatchOcrImage>): BatchOcrResponse {
        val request = buildBatchRequest(images, receiptOcrProperties.prompt)
        return objectMapper.readValue(callGemini(request), BatchOcrResponse::class.java)
    }

    // HTTP/네트워크 오류는 RestClient가 RestClientException으로 던지며 GlobalExceptionHandler가 처리한다.
    private fun callGemini(request: ReceiptOcrClientRequest): String {
        val response = restClient
            .post()
            .uri(receiptOcrProperties.url)
            .header(receiptOcrProperties.authHeader, receiptOcrProperties.apiKey)
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body(ReceiptOcrClientResponse::class.java)
            ?: throw BusinessException("API 응답이 비어있습니다.")

        return response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw BusinessException("API 응답에서 분석 결과를 찾을 수 없습니다.")
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
