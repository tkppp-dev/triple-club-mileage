package com.tkppp.tripleclubmileage.error

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalControllerExceptionHandler {
    private val logger = LoggerFactory.getLogger(GlobalControllerExceptionHandler::class.java)

    @ExceptionHandler(value = [CustomException::class])
    fun customExceptionHandler(ex: CustomException): ResponseEntity<ErrorResponseDto>{
        val error = ex.errorCode
        logger.error("${error.name} - ${error.message}")
        return ResponseEntity(ErrorResponseDto(error.name, error.message), error.status)
    }

    @ExceptionHandler(value = [RuntimeException::class])
    fun globalExceptionHandler(ex: RuntimeException): ResponseEntity<ErrorResponseDto> {
        val error = ErrorCode.INTERNAL_SERVER_ERROR
        logger.error(ex.message)
        return ResponseEntity(ErrorResponseDto(error.name, error.message), error.status)
    }
}