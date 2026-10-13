package depromeet.hotsix.obrit.notification.entity

/**
 * 한 사용자에게 보낸 푸시의 결과.
 *
 * 발송 이력과 알림 상태(지연 스텝, 여분 부족 기록)를 확정해도 되는지 호출부가 판단하려면
 * 전송이 실제로 성공했는지 알아야 한다. 실패를 성공으로 확정하면 재발송 기회가 사라진다.
 */
data class FcmSendResult(val outcome: FcmSendOutcome, val sentCount: Int, val failedCount: Int) {

    companion object {
        fun noDevice(): FcmSendResult = FcmSendResult(FcmSendOutcome.NO_DEVICE, sentCount = 0, failedCount = 0)

        /**
         * 기기 한 대라도 성공하면 발송된 것으로 본다. 사용자는 이미 알림을 받았으므로
         * 재발송하면 중복이 된다. 실패한 기기는 로그로만 남긴다.
         *
         * 만료돼 삭제된 기기는 성공도 실패도 아니다. 재시도해도 결과가 같으므로 [NO_DEVICE]로 수렴한다.
         */
        fun of(sentCount: Int, failedCount: Int): FcmSendResult = FcmSendResult(
            outcome = when {
                sentCount > 0 -> FcmSendOutcome.SENT
                failedCount > 0 -> FcmSendOutcome.FAILED
                else -> FcmSendOutcome.NO_DEVICE
            },
            sentCount = sentCount,
            failedCount = failedCount,
        )
    }
}

enum class FcmSendOutcome {
    SENT,

    /** 전송을 시도했으나 모두 실패했다. 상태를 확정하지 않고 다음 배치에서 재시도한다. */
    FAILED,

    /** 보낼 기기가 없다. 실패가 아니라 대상 제외이며, 재시도해도 결과가 같다. */
    NO_DEVICE,
}
