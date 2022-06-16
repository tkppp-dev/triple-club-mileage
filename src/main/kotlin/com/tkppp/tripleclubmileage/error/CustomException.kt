package com.tkppp.tripleclubmileage.error

class CustomException(val errorCode: ErrorCode) : RuntimeException(errorCode.message)