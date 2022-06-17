package com.tkppp.tripleclubmileage.mileage.controller

import com.tkppp.tripleclubmileage.mileage.dto.MessageResponseDto
import com.tkppp.tripleclubmileage.mileage.dto.MileageLogResponseDto
import com.tkppp.tripleclubmileage.mileage.dto.MileageSaveRequestDto
import com.tkppp.tripleclubmileage.mileage.dto.ValueResponseDto
import com.tkppp.tripleclubmileage.mileage.service.MileageService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController

class MileageController(
    private val mileageService: MileageService
) {

    @PostMapping("/events")
    fun getMileageSaveEvents(@RequestBody mileageSaveRequestDto: MileageSaveRequestDto): ResponseEntity<MessageResponseDto>{
        mileageService.saveMileagePoint(mileageSaveRequestDto)
        val message = "MILEAGE_${mileageSaveRequestDto.action.fullname}_SUCCESS"
        return ResponseEntity(MessageResponseDto(message), HttpStatus.OK)
    }

    @GetMapping("/api/mileage/{userId}")
    fun returnMileagePoint(@PathVariable userId: UUID): ResponseEntity<ValueResponseDto>{
        val point = mileageService.getMileagePoint(userId)
        return ResponseEntity(ValueResponseDto(point), HttpStatus.OK)
    }

    @GetMapping("/api/mileage/all/{userId}")
    fun returnMileageLog(@PathVariable userId: UUID): ResponseEntity<List<MileageLogResponseDto>> {
        val logs = mileageService.getMileageLogList(userId)
        return ResponseEntity(logs, HttpStatus.OK)
    }
}