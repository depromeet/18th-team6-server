package depromeet.hotsix.obrit.receipt.entity

import depromeet.hotsix.obrit.global.exception.BusinessException
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ReceiptImageTest {

    @Test
    fun `허용된_확장자로_생성하면_MIME_타입을_확정한다`() {
        val image = ReceiptImage.from("data".toByteArray(), "receipt.png")

        assertEquals("image/png", image.mimeType)
    }

    @Test
    fun `파일명이_null이면_예외를_던진다`() {
        assertFailsWith<BusinessException> {
            ReceiptImage.from("data".toByteArray(), null)
        }
    }

    @Test
    fun `허용되지_않은_확장자면_예외를_던진다`() {
        assertFailsWith<BusinessException> {
            ReceiptImage.from("data".toByteArray(), "receipt.gif")
        }
    }

    @Test
    fun `크기가_10MB를_초과하면_예외를_던진다`() {
        val oversize = ByteArray(10 * 1024 * 1024 + 1)

        assertFailsWith<BusinessException> {
            ReceiptImage.from(oversize, "receipt.jpg")
        }
    }

    @Test
    fun `빈_파일이면_예외를_던진다`() {
        assertFailsWith<BusinessException> {
            ReceiptImage.from(ByteArray(0), "receipt.jpg")
        }
    }
}
