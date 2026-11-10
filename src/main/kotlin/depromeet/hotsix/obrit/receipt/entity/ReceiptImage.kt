package depromeet.hotsix.obrit.receipt.entity

import depromeet.hotsix.obrit.global.exception.BusinessException

/**
 * 영수증 이미지 값 객체. 생성 시점에 확장자·크기·빈 파일 여부를 검증하고 MIME 타입을 확정한다.
 * 인스턴스가 존재하면 유효한 영수증 이미지임이 보장된다.
 */
class ReceiptImage private constructor(val bytes: ByteArray, val mimeType: String) {
    companion object {
        private const val MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024
        private val EXTENSION_TO_MIME_TYPE = mapOf(
            "jpg" to "image/jpeg",
            "jpeg" to "image/jpeg",
            "png" to "image/png",
            "webp" to "image/webp",
            "heic" to "image/heic",
            "heif" to "image/heif",
        )

        fun from(bytes: ByteArray, originalFilename: String?): ReceiptImage {
            val filename = originalFilename
                ?: throw BusinessException("파일명이 없습니다.")

            val extension = filename.substringAfterLast('.', "").lowercase()
            val mimeType = EXTENSION_TO_MIME_TYPE[extension]
                ?: throw BusinessException("허용되지 않은 파일 확장자입니다. (허용: jpg, jpeg, png, webp, heic, heif)")

            if (bytes.size > MAX_IMAGE_SIZE_BYTES) {
                throw BusinessException("파일 크기가 10MB를 초과합니다.")
            }

            if (bytes.isEmpty()) {
                throw BusinessException("빈 파일입니다.")
            }

            return ReceiptImage(bytes, mimeType)
        }
    }
}
