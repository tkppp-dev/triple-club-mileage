package com.tkppp.tripleclubmileage.mileage.service

import com.tkppp.tripleclubmileage.error.CustomException
import com.tkppp.tripleclubmileage.error.ErrorCode
import com.tkppp.tripleclubmileage.mileage.domain.*
import com.tkppp.tripleclubmileage.mileage.dto.MileageLogResponseDto
import com.tkppp.tripleclubmileage.mileage.dto.MileageSaveRequestDto
import com.tkppp.tripleclubmileage.mileage.util.LogStatus
import com.tkppp.tripleclubmileage.mileage.util.ReviewAction.*
import org.slf4j.LoggerFactory
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

    fun isFirstReview(placeId: UUID): Boolean {
        val result = mileageLogRepository.findGroupByAction(placeId)
        return result[0].cnt == result[1].cnt
    }

    fun getTotalPoint(cp: Int, ip: Int, bp: Int) = cp + ip + bp

    fun getMileageSavingDataWhenActionAdd(dto: MileageSaveRequestDto): Triple<Mileage, MileageLog, Int> {
        val mileage = mileageRepository.findByUserId(dto.userId) ?: Mileage(userId = dto.userId)
        val (contentPoint, imagePoint) = getPoints(dto)
        val bonusPoint = if(isFirstReview(dto.placeId)) 1 else 0
        val variation = getTotalPoint(contentPoint, imagePoint, bonusPoint)
        val log = MileageLog(
            action = dto.action,
            status = LogStatus.INCREASE,
            contentPoint = contentPoint,
            imagePoint = imagePoint,
            bonusPoint = bonusPoint,
            variation = variation,
            userId = dto.userId,
            placeId = dto.placeId
        )
        return Triple(mileage, log, variation)
    }

    fun getMileageSavingDataWhenActionMod(dto: MileageSaveRequestDto): Triple<Mileage, MileageLog, Int> {
        val mileage = mileageRepository.findByUserId(dto.userId)
            ?: throw CustomException(ErrorCode.MILEAGE_DATA_NOT_FOUND)
        val recentLog = mileageLogRepository.findRecentLog(dto.userId, dto.placeId)
            ?: throw CustomException(ErrorCode.MILEAGE_LOG_NOT_FOUND)

        val (contentPoint, imagePoint) = getPoints(dto)
        val variation = getTotalPoint(contentPoint, imagePoint, recentLog.bonusPoint) - recentLog.getTotalPoint()
        val status = if (variation > 0) {
            LogStatus.INCREASE
        } else if (variation == 0) {
            LogStatus.SAME
        } else {
            LogStatus.DECREASE
        }

        val log = MileageLog(
            action = dto.action,
            status = status,
            contentPoint = contentPoint,
            imagePoint = imagePoint,
            bonusPoint = recentLog.bonusPoint,
            variation = variation,
            userId = dto.userId,
            placeId = dto.placeId
        )

        return Triple(mileage, log, variation)
    }

    fun getMileageSavingDataWhenActionDelete(dto: MileageSaveRequestDto): Triple<Mileage, MileageLog, Int> {
        val mileage = mileageRepository.findByUserId(dto.userId)
            ?: throw CustomException(ErrorCode.MILEAGE_DATA_NOT_FOUND)
        val recentLog = mileageLogRepository.findRecentLog(dto.userId, dto.placeId)
            ?: throw CustomException(ErrorCode.MILEAGE_LOG_NOT_FOUND)

        val variation = -recentLog.getTotalPoint()
        val log = MileageLog(
            action = dto.action,
            status = LogStatus.DECREASE,
            variation = variation,
            userId = dto.userId,
            placeId = dto.placeId
        )

        return Triple(mileage, log, variation)
    }

    @Transactional
    fun saveMileagePoint(dto: MileageSaveRequestDto) {
        val (mileageEntity, logEntity, variation) = when (dto.action) {
            ADD -> getMileageSavingDataWhenActionAdd(dto)
            MOD -> getMileageSavingDataWhenActionMod(dto)
            DELETE -> getMileageSavingDataWhenActionDelete(dto)
        }

        // log save
        mileageLogRepository.save(logEntity)
        // mileage save
        mileageEntity.point += variation
        mileageRepository.save(mileageEntity)
    }
}