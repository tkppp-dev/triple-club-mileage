package com.tkppp.tripleclubmileage.mileage.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface MileageRepository: JpaRepository<Mileage, UUID> {
    fun findByUserId(userId: UUID): Mileage?
}