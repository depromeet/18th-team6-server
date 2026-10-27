package depromeet.hotsix.obrit.receipt.dto

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import tools.jackson.databind.ObjectMapper
import kotlin.test.assertEquals

@SpringBootTest
@ActiveProfiles("test")
class BatchOcrResponseParsingTest {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `배치_응답_JSON을_receipt_id와_items까지_매핑한다`() {
        val json = """
            {"results":[
              {"receipt_id":"7","store":"마트","date":"2026-01-01",
               "items":[{"original_name":"칫솔 4개입","category":"칫솔","effective_quantity":4,
                         "quantity_reason":"4개입","suggested_replacement_interval_days":90}],
               "total":10000}
            ]}
        """.trimIndent()

        val response = objectMapper.readValue(json, BatchOcrResponse::class.java)

        val first = response.results.first()
        assertEquals("7", first.receiptId)
        assertEquals("칫솔 4개입", first.items.first().original_name)
        assertEquals(4, first.items.first().effective_quantity)
    }
}
