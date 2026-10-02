package depromeet.hotsix.obrit.notification

import depromeet.hotsix.obrit.notification.entity.Notification
import depromeet.hotsix.obrit.notification.entity.NotificationEntry
import depromeet.hotsix.obrit.notification.entity.NotificationType
import depromeet.hotsix.obrit.notification.repository.NotificationEntryRepository
import depromeet.hotsix.obrit.notification.repository.NotificationRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NotificationAcceptanceTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @Autowired
    private lateinit var notificationEntryRepository: NotificationEntryRepository

    // 묶음 알림을 조회하면 부모 요약과 소모품별 카드가 순서대로 반환되는지 확인한다.
    @Test
    fun `묶음 알림은 소모품별 카드를 함께 반환한다`() {
        val notification = notificationRepository.save(
            Notification(
                userId = USER_ID,
                type = NotificationType.OVERDUE,
                title = "교체가 필요한 소모품이 있어요",
                body = "수건 외 1건, 확인해보세요",
            ),
        )
        notificationEntryRepository.saveAll(
            listOf(
                entry(notification, NotificationType.OVERDUE, 10L, "수건", 0),
                entry(notification, NotificationType.PRE_REPLACEMENT, 20L, "칫솔", 1),
            ),
        )
        notificationEntryRepository.flush()

        mockMvc.perform(get("/notifications").header("X-User-Id", USER_ID))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].title").value("교체가 필요한 소모품이 있어요"))
            .andExpect(jsonPath("$.data[0].isBundled").value(true))
            .andExpect(jsonPath("$.data[0].cards.length()").value(2))
            .andExpect(jsonPath("$.data[0].cards[0].type").value("OVERDUE"))
            .andExpect(jsonPath("$.data[0].cards[0].itemName").value("수건"))
            .andExpect(jsonPath("$.data[0].cards[0].deepLink").value("obrit://items/10"))
            .andExpect(jsonPath("$.data[0].cards[1].type").value("PRE_REPLACEMENT"))
            .andExpect(jsonPath("$.data[0].cards[1].label").value("교체 D-3"))
    }

    private fun entry(
        notification: Notification,
        type: NotificationType,
        itemId: Long,
        itemName: String,
        displayOrder: Int,
    ) = NotificationEntry(
        notification = notification,
        type = type,
        itemId = itemId,
        title = if (type == NotificationType.OVERDUE) "교체 시기가 지났어요" else "교체 시기가 다가와요",
        body = "$itemName 알림 내용",
        itemName = itemName,
        label = if (type == NotificationType.OVERDUE) "교체 D+1" else "교체 D-3",
        nextReplacementDate = LocalDate.of(2026, 9, 25),
        displayOrder = displayOrder,
    )

    companion object {
        private const val USER_ID = 193L
    }
}
