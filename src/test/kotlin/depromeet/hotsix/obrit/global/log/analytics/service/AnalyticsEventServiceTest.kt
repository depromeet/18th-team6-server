package depromeet.hotsix.obrit.global.log.analytics.service

import depromeet.hotsix.obrit.global.log.analytics.repository.AnalyticsEventRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
class AnalyticsEventServiceTest {

    @Autowired
    private lateinit var analyticsEventService: AnalyticsEventService

    @Autowired
    private lateinit var analyticsEventRepository: AnalyticsEventRepository

    @BeforeEach
    fun cleanup() {
        analyticsEventRepository.deleteAll()
    }

    @Test
    fun `publishSignupCompleted_시_analytics_events_테이블에_저장된다`() {
        analyticsEventService.publishSignupCompleted(userId = 1L, signupMethod = "uuid")

        val saved = analyticsEventRepository.findAll()
        assertEquals(1, saved.size)
        val row = saved.first()
        assertTrue(row.eventId.isNotBlank())
        assertEquals("signup_completed", row.eventName)
        assertEquals(1L, row.userId)
        assertTrue(row.properties.contains("signup_method"), "properties=${row.properties}")
        assertTrue(row.properties.contains("uuid"), "properties=${row.properties}")
    }

    @Test
    fun `publishSignupCompleted_를_두_번_호출하면_서로_다른_event_id로_저장된다`() {
        analyticsEventService.publishSignupCompleted(userId = 2L, signupMethod = "uuid")
        analyticsEventService.publishSignupCompleted(userId = 2L, signupMethod = "uuid")

        val saved = analyticsEventRepository.findAll()
        assertEquals(2, saved.size)
        assertNotEquals(saved[0].eventId, saved[1].eventId)
    }
}
