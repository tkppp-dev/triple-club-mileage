package com.tkppp.tripleclubmileage.mileage.dto

import com.tkppp.tripleclubmileage.mileage.domain.Mileage
import com.tkppp.tripleclubmileage.mileage.domain.MileageLog
import com.tkppp.tripleclubmileage.mileage.util.LogStatus
import com.tkppp.tripleclubmileage.mileage.util.ReviewAction
import org.apache.juli.logging.Log
import java.util.*

data class MileageSaveRequestDto(
    val type: String,
    val action: ReviewAction,
    val reviewId: UUID,
    val content: String,
    val attachedPhotoIds: List<UUID>,
    val userId: UUID,
    val placeId: UUID
)
