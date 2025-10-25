package dev.rockyj.example.demo.commands

import dev.rockyj.example.demo.controllers.requests.LoginRequest
import dev.rockyj.example.demo.controllers.requests.UserRegistrationRequest
import dev.rockyj.example.demo.domain.dtos.UserDTO
import dev.rockyj.example.demo.services.AuthService
import dev.rockyj.example.demo.services.JWTService
import dev.rockyj.example.demo.utils.*
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.*

@Service
class AuthCommand(
    private val jwt: JWTService,
    private val authService: AuthService,
) {
    fun register(registrationRequest: UserRegistrationRequest): UserDTO {
        return success(registrationRequest)
            .flatMap { validRequest ->
                throwIfExists(authService.userExists(validRequest.email))
            }.flatMap {
                runWithSafety { authService.register(registrationRequest.email, registrationRequest.password) }
            }.getOrThrow()
    }

    fun login(loginRequest: LoginRequest): String {
        return success(loginRequest)
            .flatMap { validRequest ->
                runWithSafety { authService.verify(validRequest.email, validRequest.password) }
            }.flatMap { userId ->
                when (userId) {
                    is UUID -> success(userId)
                    else -> failure(ResponseStatusException(HttpStatus.BAD_REQUEST, "user not found"))
                }
            }
            .flatMap { userId ->
                val token =
                    jwt.signJWT(
                        "$userId",
                        audience = "app",
                        expirationMinutes = 60,
                        customClaims = null,
                    )
                success(token)
            }.getOrThrow()
    }

    private fun throwIfExists(userId: UUID?): Result<UUID?> {
        return if (userId == null) {
            success(userId)
        } else {
            // User already exists and cannot be created again
            failure(ResponseStatusException(HttpStatus.BAD_REQUEST, "user already exists"))
        }
    }
}
