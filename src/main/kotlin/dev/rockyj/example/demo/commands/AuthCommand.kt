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
                throwIfExists(validRequest.email) { authService.userExists(validRequest.email) }
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
                runWithSafety {
                    jwt.signJWT(
                        "$userId",
                        audience = "app",
                        expirationMinutes = 60,
                        customClaims = null,
                    )
                }
            }.getOrThrow()
    }

    private fun <T> throwIfExists(value: T, block: () -> T): Result<T> {
        return if (block() == null) {
            success(value)
        } else {
            failure(ResponseStatusException(HttpStatus.BAD_REQUEST, "user already exists"))
        }
    }
}
