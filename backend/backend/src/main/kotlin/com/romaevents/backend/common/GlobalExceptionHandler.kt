package com.romaevents.backend.common

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException

//GlobalExceptionHandler is a class annotated with @RestControllerAdvice that provides centralized exception handling for the entire application. It defines two methods to handle exceptions: one for ResponseStatusException, which is commonly used to indicate specific HTTP status codes and reasons, and another for generic Exception, which catches any unhandled exceptions. Each method constructs an appropriate ResponseEntity containing an ApiError object with the error message and returns it to the client with the corresponding HTTP status code. This allows for consistent error responses across the application and helps in debugging and user feedback.
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