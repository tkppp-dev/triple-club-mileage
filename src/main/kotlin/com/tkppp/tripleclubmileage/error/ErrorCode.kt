package com.tkppp.tripleclubmileage.error

import org.springframework.http.HttpStatus

enum class ErrorCode(val status: HttpStatus, val message: String){
    // 404 NOT_FOUND
    MILEAGE_DATA_NOT_FOUND(HttpStatus.NOT_FOUND, "마일리지 데이터를 찾을 수 없습니다"),
    MILEAGE_LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "이전 마일리지 적립 데이터를 찾을 수 없습니다"),

    // 500 INTERNAL_SERVER_ERROR
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "예상치 못한 에러가 발생했습니다")
}
