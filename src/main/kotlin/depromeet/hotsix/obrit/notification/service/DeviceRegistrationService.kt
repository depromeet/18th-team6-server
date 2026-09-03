package depromeet.hotsix.obrit.notification.service

import depromeet.hotsix.obrit.notification.dto.response.RegisterDeviceResponse
import depromeet.hotsix.obrit.notification.entity.DeviceRegistration
import depromeet.hotsix.obrit.notification.repository.DeviceRegistrationRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate

@Service
class DeviceRegistrationService(
    private val deviceRegistrationRepository: DeviceRegistrationRepository,
    transactionManager: PlatformTransactionManager,
) {
    /**
     * 각 쓰기 연산을 독립된 트랜잭션으로 실행한다.
     *
     * 삽입과 재조회를 한 트랜잭션에 묶으면 unique 제약 위반 이후 Hibernate 세션을
     * 재사용할 수 없어 동시 요청을 복구할 수 없다. 그래서 경계를 나눈다.
     */
    private val transaction = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    /** 기기를 등록한다. 이미 등록된 FID면 소유자만 현재 사용자로 갱신한다. */
    fun register(userId: Long, fid: String): RegisterDeviceResponse {
        reassignExisting(userId, fid)?.let { return it }

        return try {
            transaction.execute {
                deviceRegistrationRepository
                    .saveAndFlush(DeviceRegistration(userId = userId, fid = fid))
                    .toResponse()
            }!!
        } catch (e: DataIntegrityViolationException) {
            // 동시 요청으로 상대 트랜잭션이 먼저 삽입한 경우 그 행의 소유자를 갱신한다.
            reassignExisting(userId, fid) ?: throw e
        }
    }

    /** 로그아웃 시 호출한다. 등록되지 않았거나 다른 사용자의 기기면 아무 일도 하지 않는다. */
    fun unregister(userId: Long, fid: String) {
        transaction.executeWithoutResult {
            deviceRegistrationRepository.deleteByFidAndUserId(fid, userId)
        }
    }

    private fun reassignExisting(userId: Long, fid: String): RegisterDeviceResponse? = transaction.execute {
        deviceRegistrationRepository.findByFid(fid)
            ?.also { it.reassignOwner(userId) }
            ?.toResponse()
    }

    private fun DeviceRegistration.toResponse() = RegisterDeviceResponse(
        id = id!!,
        userId = userId,
        fid = fid,
        createdAt = createdAt!!,
    )
}
