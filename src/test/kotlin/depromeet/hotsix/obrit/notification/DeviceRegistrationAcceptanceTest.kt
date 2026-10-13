package depromeet.hotsix.obrit.notification

import depromeet.hotsix.obrit.notification.repository.DeviceRegistrationRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeviceRegistrationAcceptanceTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var deviceRegistrationRepository: DeviceRegistrationRepository

    @BeforeEach
    fun setUp() {
        deviceRegistrationRepository.deleteAllInBatch()
    }

    @Test
    fun `기기를_등록하면_DB에_저장된다`() {
        // When
        `기기를 등록한다`(userId = 1L, fid = "fid-device-abc")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))

        // Then
        val devices = deviceRegistrationRepository.findAllByUserId(1L)
        assertEquals(1, devices.size)
        assertEquals("fid-device-abc", devices[0].fid)
    }

    @Test
    fun `동일_FID를_재등록하면_중복_저장되지_않는다`() {
        // Given
        `기기를 등록한다`(userId = 1L, fid = "same-fid")
            .andExpect(status().isOk)

        // When
        `기기를 등록한다`(userId = 1L, fid = "same-fid")
            .andExpect(status().isOk)

        // Then
        assertEquals(1, deviceRegistrationRepository.findAllByUserId(1L).size)
    }

    @Test
    fun `같은_사용자가_다른_FID를_등록하면_멀티_디바이스로_저장된다`() {
        // Given
        `기기를 등록한다`(userId = 1L, fid = "fid-device-a")
            .andExpect(status().isOk)

        // When
        `기기를 등록한다`(userId = 1L, fid = "fid-device-b")
            .andExpect(status().isOk)

        // Then
        assertEquals(2, deviceRegistrationRepository.findAllByUserId(1L).size)
    }

    @Test
    fun `다른_사용자가_동일_FID를_등록하면_소유자가_변경된다`() {
        // Given: 유저1이 기기 등록
        `기기를 등록한다`(userId = 1L, fid = "shared-fid")
            .andExpect(status().isOk)

        // When: 같은 기기에 유저2가 로그인
        `기기를 등록한다`(userId = 2L, fid = "shared-fid")
            .andExpect(status().isOk)

        // Then: 유저1에게는 더 이상 발송되지 않는다
        assertTrue(deviceRegistrationRepository.findAllByUserId(1L).isEmpty())
        val user2Devices = deviceRegistrationRepository.findAllByUserId(2L)
        assertEquals(1, user2Devices.size)
        assertEquals("shared-fid", user2Devices[0].fid)
    }

    @Test
    fun `빈_FID로_등록하면_400을_반환한다`() {
        // When & Then
        `기기를 등록한다`(userId = 1L, fid = "")
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `등록을_해제하면_해당_기기가_삭제된다`() {
        // Given
        `기기를 등록한다`(userId = 1L, fid = "fid-to-remove")
            .andExpect(status().isOk)

        // When
        `기기 등록을 해제한다`(userId = 1L, fid = "fid-to-remove")
            .andExpect(status().isOk)

        // Then
        assertTrue(deviceRegistrationRepository.findAllByUserId(1L).isEmpty())
    }

    @Test
    fun `다른_사용자의_기기는_등록_해제되지_않는다`() {
        // Given: 유저1의 기기
        `기기를 등록한다`(userId = 1L, fid = "user1-fid")
            .andExpect(status().isOk)

        // When: 유저2가 해제 시도
        `기기 등록을 해제한다`(userId = 2L, fid = "user1-fid")
            .andExpect(status().isOk)

        // Then: 유저1의 기기는 그대로 유지된다
        assertEquals(1, deviceRegistrationRepository.findAllByUserId(1L).size)
    }

    private fun `기기를 등록한다`(userId: Long, fid: String) = mockMvc.perform(
        post("/notifications/devices")
            .header("X-User-Id", userId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"fid": "$fid"}"""),
    )

    private fun `기기 등록을 해제한다`(userId: Long, fid: String) = mockMvc.perform(
        delete("/notifications/devices/{fid}", fid)
            .header("X-User-Id", userId),
    )
}
