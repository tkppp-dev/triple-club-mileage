package com.tkppp.tripleclubmileage.mileage.service

import com.tkppp.tripleclubmileage.error.CustomException
import com.tkppp.tripleclubmileage.error.ErrorCode
import com.tkppp.tripleclubmileage.mileage.domain.*
import com.tkppp.tripleclubmileage.mileage.dto.MileageLogResponseDto
import com.tkppp.tripleclubmileage.mileage.dto.MileageSaveRequestDto
import com.tkppp.tripleclubmileage.mileage.util.LogStatus
import com.tkppp.tripleclubmileage.mileage.util.ReviewAction
import com.tkppp.tripleclubmileage.mileage.util.ReviewAction.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class MileageService(
    private val mileageRepository: MileageRepository,
    private val mileageLogRepository: MileageLogRepository
) {

    fun getMileagePoint(userId: UUID): Int =
        when (val entity = mileageRepository.findByUserId(userId)) {
            null -> throw CustomException(ErrorCode.MILEAGE_DATA_NOT_FOUND)
            else -> entity.point
        }

    fun getMileageLogList(userId: UUID): List<MileageLogResponseDto> {
        val logs = mileageLogRepository.findByUserId(userId)
        return logs.map { MileageLogResponseDto(it) }
    }

    fun getPoints(dto: MileageSaveRequestDto): Pair<Int, Int> {
        val contentPoint = if (dto.content.isNotEmpty()) 1 else 0
        val imagePoint = if (dto.attachedPhotoIds.isNotEmpty()) 1 else 0

        return Pair(contentPoint, imagePoint)
    }

    fun getTotalPoint(cp: Int, ip: Int, bp: Int) = cp + ip + bp

    fun getMileageEntity(action: ReviewAction, userId: UUID): Mileage = when (action) {
        ADD -> mileageRepository.findByUserId(userId) ?: Mileage(userId = userId)
        else -> mileageRepository.findByUserId(userId)
            ?: throw CustomException(ErrorCode.MILEAGE_DATA_NOT_FOUND)
    }

    fun getRecentLog(reviewId: UUID): MileageLog =
        mileageLogRepository.findRecentLog(reviewId) ?: throw CustomException(ErrorCode.MILEAGE_LOG_NOT_FOUND)

    fun isFirstReview(placeId: UUID): Boolean {
        val result = mileageLogRepository.findGroupByAction(placeId)
        return if (result.isEmpty()) {
            true
        } else if (result.size == 1) {
            false
        } else {
            result[0].cnt == result[1].cnt
        }
    }

    fun getMileageLogEntityWhenActionAdd(dto: MileageSaveRequestDto): MileageLog {
        val (contentPoint, imagePoint) = getPoints(dto)
        val bonusPoint = if (isFirstReview(dto.placeId)) 1 else 0
        val variation = getTotalPoint(contentPoint, imagePoint, bonusPoint)

        return MileageLog(
            action = dto.action,
            status = LogStatus.INCREASE,
            contentPoint = contentPoint,
            imagePoint = imagePoint,
            bonusPoint = bonusPoint,
            variation = variation,
            userId = dto.userId,
            placeId = dto.placeId,
            reviewId = dto.reviewId
        )
    }

    fun getMileageLogEntityWhenActionMod(dto: MileageSaveRequestDto): MileageLog {
        val recentLog = getRecentLog(dto.reviewId)
        val (contentPoint, imagePoint) = getPoints(dto)
        val variation = getTotalPoint(contentPoint, imagePoint, recentLog.bonusPoint) - recentLog.getTotalPoint()
        val status = if (variation > 0) {
            LogStatus.INCREASE
        } else if (variation == 0) {
            LogStatus.SAME
        } else {
            LogStatus.DECREASE
        }

        return MileageLog(
            action = dto.action,
            status = status,
            contentPoint = contentPoint,
            imagePoint = imagePoint,
            bonusPoint = recentLog.bonusPoint,
            variation = variation,
            userId = dto.userId,
            placeId = dto.placeId,
            reviewId = dto.reviewId
        )
    }

    fun getMileageLogEntityWhenActionDelete(dto: MileageSaveRequestDto): MileageLog {
        val recentLog = getRecentLog(dto.reviewId)
        val variation = -recentLog.getTotalPoint()

        return MileageLog(
            action = dto.action,
            status = LogStatus.DECREASE,
            variation = variation,
            userId = dto.userId,
            placeId = dto.placeId,
            reviewId = dto.reviewId
        )
    }

    @Transactional
    fun saveMileagePoint(dto: MileageSaveRequestDto) {
        val mileageEntity = getMileageEntity(dto.action, dto.userId)
        val logEntity = when (dto.action) {
            ADD -> getMileageLogEntityWhenActionAdd(dto)
            MOD -> getMileageLogEntityWhenActionMod(dto)
            DELETE -> getMileageLogEntityWhenActionDelete(dto)
        }

        mileageEntity.point += logEntity.variation
        mileageRepository.save(mileageEntity)
        mileageLogRepository.save(logEntity)
    }
}