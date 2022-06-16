package com.tkppp.tripleclubmileage.mileage.dto

import com.tkppp.tripleclubmileage.mileage.domain.MileageLog
import com.tkppp.tripleclubmileage.mileage.util.LogStatus
import com.tkppp.tripleclubmileage.mileage.util.ReviewAction
import java.time.LocalDateTime

data class MileageLogResponseDto(
    val action: ReviewAction,
    val status: LogStatus,
    val variation: Int,
    val createdAt: LocalDateTime
) {
    constructor(log: MileageLog) : this(
        action = log.action,
        status = log.status,
        variation = log.variation,
        createdAt = log.createdAt
    )
}