package com.tkppp.tripleclubmileage.mileage.controller

import com.tkppp.tripleclubmileage.mileage.dto.MessageResponseDto
import com.tkppp.tripleclubmileage.mileage.dto.MileageLogResponseDto
import com.tkppp.tripleclubmileage.mileage.dto.MileageSaveRequestDto
import com.tkppp.tripleclubmileage.mileage.dto.ValueResponseDto
import com.tkppp.tripleclubmileage.mileage.service.MileageService
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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
    @Operation(description = "리뷰 작성, 수정, 삭제 이벤트 발생시 마일리지 포인트 부여")
    fun getMileageSaveEvents(@RequestBody mileageSaveRequestDto: MileageSaveRequestDto): ResponseEntity<MessageResponseDto> {
        mileageService.saveMileagePoint(mileageSaveRequestDto)
        val message = "MILEAGE_${mileageSaveRequestDto.action.fullname}_SUCCESS"
        return ResponseEntity(MessageResponseDto(message), HttpStatus.OK)
    }

    @GetMapping("/api/mileage/{userId}")
    @Operation(description = "유저의 현재 포인트 정보 반환")
    fun returnMileagePoint(@PathVariable userId: UUID): ResponseEntity<ValueResponseDto> {
        val point = mileageService.getMileagePoint(userId)
        return ResponseEntity(ValueResponseDto(point), HttpStatus.OK)
    }

    @GetMapping("/api/mileage/all/{userId}")
    @Operation(description = "유저의 포인트 증감 이력 반환")
    fun returnMileageLog(@PathVariable userId: UUID): ResponseEntity<List<MileageLogResponseDto>> {
        val logs = mileageService.getMileageLogList(userId)
        return ResponseEntity(logs, HttpStatus.OK)
    }
}