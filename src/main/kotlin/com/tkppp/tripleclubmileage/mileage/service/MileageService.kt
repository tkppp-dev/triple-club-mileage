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

    fun getMileagePoint(userId: UUID): Int {
        return when (val entity = mileageRepository.findByUserId(userId)) {
            null -> throw CustomException(ErrorCode.MILEAGE_DATA_NOT_FOUND)
            else -> entity.point
        }
    }

    fun getMileageLogList(userId: UUID): List<MileageLogResponseDto> {
        val logs = mileageLogRepository.findByUserId(userId)
        return logs.map { MileageLogResponseDto(it) }
    }

    fun isFirstReview(placeId: UUID): Boolean {
        val result = mileageLogRepository.findGroupByAction(placeId)
        return result[0].cnt == result[1].cnt
    }

    fun getPoints(dto: MileageSaveRequestDto): Triple<Int, Int, Int> {
        val contentPoint = if (dto.content.isNotEmpty()) 1 else 0
        val imagePoint = if (dto.attachedPhotoIds.isNotEmpty()) 1 else 0
        val bonusPoint = if (isFirstReview(dto.placeId)) 1 else 0

        return Triple(contentPoint, imagePoint, bonusPoint)
    }

    fun getTotalPoint(point: Triple<Int, Int, Int>) = point.first + point.second + point.third

    @Transactional
    fun saveMileagePoint(dto: MileageSaveRequestDto) {
        val (mileage, variation, log) = when (dto.action) {
            ADD -> {
                val mileage = mileageRepository.findByUserId(dto.userId) ?: Mileage(userId = dto.userId)
                val point = getPoints(dto)
                val variation = getTotalPoint(point)
                val log = MileageLog(
                    action = dto.action,
                    status = LogStatus.INCREASE,
                    contentPoint = point.first,
                    imagePoint = point.second,
                    bonusPoint = point.third,
                    variation = variation,
                    userId = dto.userId,
                    placeId = dto.placeId
                )
                Triple(mileage, variation, log)
            }
            MOD -> {
                val mileage = mileageRepository.findByUserId(dto.userId)
                    ?: throw CustomException(ErrorCode.MILEAGE_DATA_NOT_FOUND)
                val recentLog = mileageLogRepository.findRecentLog(dto.userId, dto.placeId)
                    ?: throw CustomException(ErrorCode.MILEAGE_LOG_NOT_FOUND)

                val point = getPoints(dto)
                val variation = recentLog.getTotalPoint() - getTotalPoint(point)
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
                    contentPoint = point.first,
                    imagePoint = point.second,
                    bonusPoint = point.third,
                    variation = variation,
                    userId = dto.userId,
                    placeId = dto.placeId
                )

                Triple(mileage, variation, log)
            }
            DELETE -> {
                val mileage = mileageRepository.findByUserId(dto.userId)
                    ?: throw CustomException(ErrorCode.MILEAGE_DATA_NOT_FOUND)
                val recentLog = mileageLogRepository.findRecentLog(dto.userId, dto.placeId)
                    ?: throw CustomException(ErrorCode.MILEAGE_LOG_NOT_FOUND)

                val variation = recentLog.getTotalPoint()
                val log = MileageLog(
                    action = dto.action,
                    status = LogStatus.DECREASE,
                    variation = variation,
                    userId = dto.userId,
                    placeId = dto.placeId
                )

                Triple(mileage, variation, log)
            }
        }

        // mileage save
        mileage.point += variation
        mileageRepository.save(mileage)
        // log sve
        mileageLogRepository.save(log)
    }

}