package depromeet.hotsix.obrit.receipt.controller

import depromeet.hotsix.obrit.global.dto.ApiResponse
import depromeet.hotsix.obrit.receipt.controller.docs.ReceiptJobControllerApi
import depromeet.hotsix.obrit.receipt.dto.EnqueueReceiptJobResponse
import depromeet.hotsix.obrit.receipt.dto.ReceiptJobResponse
import depromeet.hotsix.obrit.receipt.service.ReceiptJobService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/receipts/jobs")
class ReceiptJobController(private val receiptJobService: ReceiptJobService) : ReceiptJobControllerApi {

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.ACCEPTED)
    override fun enqueue(
        @RequestHeader("X-User-Id") userId: Long,
        @RequestParam("image") imageFile: MultipartFile,
    ): ApiResponse<EnqueueReceiptJobResponse> =
        ApiResponse.ok(EnqueueReceiptJobResponse(receiptJobService.enqueue(userId, imageFile)))

    @GetMapping("/{jobId}")
    override fun getJob(@PathVariable jobId: Long): ApiResponse<ReceiptJobResponse> =
        ApiResponse.ok(receiptJobService.getJob(jobId))
}
