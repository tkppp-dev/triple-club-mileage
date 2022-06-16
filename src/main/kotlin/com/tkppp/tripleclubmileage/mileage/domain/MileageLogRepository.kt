package com.tkppp.tripleclubmileage.mileage.domain

import com.tkppp.tripleclubmileage.mileage.dto.AddDeleteCountingDto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.*

fun MileageLogRepository.findRecentLog(userId: UUID, placeId: UUID) =
    findTop1ByUserIdAndPlaceIdOrderByCreatedAtDesc(userId, placeId)

interface MileageLogRepository : JpaRepository<MileageLog, UUID> {

    fun findByUserId(userId: UUID): List<MileageLog>
    fun findTop1ByUserIdAndPlaceIdOrderByCreatedAtDesc(userId: UUID, placeId: UUID): MileageLog?
    @Query(
        """
        select new com.tkppp.tripleclubmileage.mileage.dto.AddDeleteCountingDto(temp.action, count(temp))
        from MileageLog as temp
        where temp.action <> 'MOD' and temp.placeId = :placeId
        group by temp.action
    """
    )
    fun findGroupByAction(placeId: UUID): List<AddDeleteCountingDto>

}