package com.romaevents.backend.auth

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.Base64

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val jwtService: JwtService
) {

    fun register(request: RegisterRequest): AuthResponse {
        val username = request.username.trim()
        val email = request.email.trim().lowercase()
        val password = request.password

        if (username.length < 3) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Username troppo corto")
        }

        if (!email.contains("@")) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Email non valida")
        }

        if (password.length < 6) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Password troppo corta")
        }

        if (userRepository.existsByEmail(email)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Email già registrata")
        }

        if (userRepository.existsByUsername(username)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Username già utilizzato")
        }

        val user = userRepository.save(
            User(
                username = username,
                email = email,
                passwordHash = hashPassword(password)
            )
        )

        return AuthResponse(
            userId = user.id,
            username = user.username,
            email = user.email,
            token = jwtService.generateToken(user)
        )
    }

    fun login(request: LoginRequest): AuthResponse {
        val email = request.email.trim().lowercase()
        val password = request.password

        val user = userRepository.findByEmail(email)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email non registrata")

        if (user.passwordHash != hashPassword(password)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Password non valida")
        }

        return AuthResponse(
            userId = user.id,
            username = user.username,
            email = user.email,
            token = jwtService.generateToken(user)
        )
    }

    private fun hashPassword(password: String): String {
        return Base64.getEncoder().encodeToString(password.toByteArray())
    }
}