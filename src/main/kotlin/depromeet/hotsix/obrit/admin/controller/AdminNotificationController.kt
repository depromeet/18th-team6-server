package depromeet.hotsix.obrit.admin.controller

import depromeet.hotsix.obrit.admin.dto.AdminNoticeForm
import depromeet.hotsix.obrit.admin.dto.AdminNotificationSettingsForm
import depromeet.hotsix.obrit.admin.service.AdminNotificationService
import depromeet.hotsix.obrit.global.exception.BusinessException
import depromeet.hotsix.obrit.global.exception.ConflictException
import depromeet.hotsix.obrit.global.exception.ResourceNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.mvc.support.RedirectAttributes

@Controller
@RequestMapping("/admin/notifications")
class AdminNotificationController(private val adminNotificationService: AdminNotificationService) {

    private val log = LoggerFactory.getLogger(AdminNotificationController::class.java)

    @GetMapping
    fun dashboard(model: Model): String {
        val dashboard = adminNotificationService.getDashboard()
        model.addAttribute("activeMenu", "notifications")
        model.addAttribute("coverage", dashboard.coverage)
        model.addAttribute("settings", dashboard.settings)
        model.addAttribute("preview", dashboard.preview)
        return "admin/notifications"
    }

    @PostMapping("/settings")
    fun updateSettings(
        @ModelAttribute form: AdminNotificationSettingsForm,
        redirectAttributes: RedirectAttributes,
    ): String = runAdminAction(redirectAttributes) {
        adminNotificationService.updateSettings(form)
        "정책 설정을 저장했습니다."
    }

    @PostMapping("/auto-dispatch")
    fun updateAutoDispatch(@RequestParam enabled: Boolean, redirectAttributes: RedirectAttributes): String =
        runAdminAction(redirectAttributes) {
            adminNotificationService.updateAutoDispatch(enabled)
            if (enabled) "자동 발송을 켰습니다. 다음 발송 시각부터 알림이 나갑니다." else "자동 발송을 껐습니다."
        }

    @PostMapping("/dispatch")
    fun dispatchNow(redirectAttributes: RedirectAttributes): String = runAdminAction(redirectAttributes) {
        val count = adminNotificationService.dispatchNow()
        "정책 배치를 실행했습니다. 발송 사용자 ${count}명."
    }

    @PostMapping("/notice")
    fun sendNotice(@ModelAttribute form: AdminNoticeForm, redirectAttributes: RedirectAttributes): String =
        runAdminAction(redirectAttributes) {
            val count = adminNotificationService.sendNotice(form)
            "공지를 발송했습니다. 발송 사용자 ${count}명."
        }

    private fun runAdminAction(redirectAttributes: RedirectAttributes, action: () -> String): String = try {
        redirectAttributes.addFlashAttribute("message", action())
        REDIRECT_TO
    } catch (e: BusinessException) {
        redirectAttributes.addFlashAttribute("error", e.message ?: "관리자 작업에 실패했습니다.")
        REDIRECT_TO
    } catch (e: ResourceNotFoundException) {
        redirectAttributes.addFlashAttribute("error", e.message ?: "관리자 작업에 실패했습니다.")
        REDIRECT_TO
    } catch (e: ConflictException) {
        redirectAttributes.addFlashAttribute("error", e.message ?: "관리자 작업에 실패했습니다.")
        REDIRECT_TO
    } catch (e: Exception) {
        // 예상 못한 예외로 흰 오류 페이지를 띄우면 관리 화면 자체를 못 쓰게 된다.
        log.error("알림 관리자 작업 실패", e)
        redirectAttributes.addFlashAttribute("error", "관리자 작업에 실패했습니다.")
        REDIRECT_TO
    }

    companion object {
        private const val REDIRECT_TO = "redirect:/admin/notifications"
    }
}
