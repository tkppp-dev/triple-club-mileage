package com.tkppp.tripleclubmileage.mileage.dto

import com.tkppp.tripleclubmileage.mileage.util.ReviewAction

data class AddDeleteCountingDto(
    val action: ReviewAction,
    val cnt: Long
)