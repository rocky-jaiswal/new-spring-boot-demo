package dev.rockyj.example.demo.controllers

import dev.rockyj.example.demo.commands.AuthCommand
import dev.rockyj.example.demo.controllers.requests.LoginRequest
import dev.rockyj.example.demo.controllers.requests.UserRegistrationRequest
import dev.rockyj.example.demo.domain.dtos.UserDTO
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@Validated
@RestController
@RequestMapping("/api/v1/auth")
class AuthController(private val authCommand: AuthCommand) {

    private val log = LoggerFactory.getLogger(AuthController::class.java)

    @PostMapping("/register")
    fun registerUser(@Valid @RequestBody userRegistrationRequest: UserRegistrationRequest): UserDTO {
        log.info("request to find or create user") // TODO: Use AOP

        return authCommand.register(userRegistrationRequest)
    }

    @PostMapping("/login")
    fun createLogin(@Valid @RequestBody loginRequest: LoginRequest): String {
        log.info("request to find or create user") // TODO: Use AOP

        return authCommand.login(loginRequest)
    }
}