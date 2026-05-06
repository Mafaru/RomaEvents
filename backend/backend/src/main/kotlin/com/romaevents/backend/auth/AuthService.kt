package com.romaevents.backend.auth

import org.springframework.stereotype.Service
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
            throw RuntimeException("Username troppo corto")
        }

        if (!email.contains("@")) {
            throw RuntimeException("Email non valida")
        }

        if (password.length < 6) {
            throw RuntimeException("Password troppo corta")
        }

        if (userRepository.existsByEmail(email)) {
            throw RuntimeException("Email già registrata")
        }

        if (userRepository.existsByUsername(username)) {
            throw RuntimeException("Username già utilizzato")
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
            ?: throw RuntimeException("Credenziali non valide")

        if (user.passwordHash != hashPassword(password)) {
            throw RuntimeException("Credenziali non valide")
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