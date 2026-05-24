package com.romaevents.backend.common

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(
        ex: ResponseStatusException
    ): ResponseEntity<ApiError> {
        return ResponseEntity
            .status(ex.statusCode)
            .body(ApiError(ex.reason ?: "Errore"))
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception
    ): ResponseEntity<ApiError> {
        return ResponseEntity
            .internalServerError()
            .body(ApiError(ex.message ?: "Errore interno del server"))
    }
}