package depromeet.hotsix.obrit.receipt.controller

import depromeet.hotsix.obrit.global.dto.ApiResponse
import depromeet.hotsix.obrit.receipt.controller.docs.ReceiptScanControllerApi
import depromeet.hotsix.obrit.receipt.dto.CreateReceiptScanResponse
import depromeet.hotsix.obrit.receipt.dto.ReceiptScanResponse
import depromeet.hotsix.obrit.receipt.service.ReceiptJobService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.net.URI

@RestController
@RequestMapping("/receipts/scans")
class ReceiptScanController(private val receiptJobService: ReceiptJobService) : ReceiptScanControllerApi {

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    override fun createScan(
        @RequestHeader("X-User-Id") userId: Long,
        @RequestParam("image") imageFile: MultipartFile,
    ): ResponseEntity<ApiResponse<CreateReceiptScanResponse>> {
        val scanId = receiptJobService.enqueue(userId, imageFile)
        return ResponseEntity.accepted()
            .location(URI.create("/receipts/scans/$scanId"))
            .body(ApiResponse.ok(CreateReceiptScanResponse(scanId)))
    }

    @GetMapping("/{scanId}")
    override fun getScan(
        @RequestHeader("X-User-Id") userId: Long,
        @PathVariable scanId: Long,
    ): ApiResponse<ReceiptScanResponse> = ApiResponse.ok(receiptJobService.getJob(userId, scanId))
}
