package dev.rockyj.example.demo.commands

import dev.rockyj.example.demo.controllers.requests.LoginRequest
import dev.rockyj.example.demo.controllers.requests.UserRegistrationRequest
import dev.rockyj.example.demo.domain.dtos.UserDTO
import dev.rockyj.example.demo.services.AuthService
import dev.rockyj.example.demo.services.JWTService
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

class AuthCommandTest {

    private val mockedJWTService = mockk<JWTService>()
    private val mockedAuthService = mockk<AuthService>()

    private val authCommand = AuthCommand(mockedJWTService, mockedAuthService)

    @BeforeEach
    fun cleanup() {
        clearAllMocks()
    }

    @Test
    fun test_register_when_user_not_found() {
        every { mockedAuthService.userExists(any(String::class)) } returns UUID.randomUUID()

        assertThatThrownBy {
            authCommand.register(
                UserRegistrationRequest(
                    "foo@example.com",
                    "12345678",
                    "12345678"
                )
            )
        }.hasMessageContaining("user already exists")
    }

    @Test
    fun test_register_when_user_creation_fails() {
        every { mockedAuthService.userExists(any(String::class)) } returns null
        every {
            mockedAuthService.register(
                any(String::class),
                any(String::class)
            )
        } throws RuntimeException("bad db")

        assertThatThrownBy {
            authCommand.register(
                UserRegistrationRequest(
                    "foo@example.com",
                    "12345678",
                    "12345678"
                )
            )
        }.hasMessageContaining("bad db")
    }

    @Test
    fun test_register_when_user_creation_works() {
        val user = UserDTO(UUID.randomUUID(), "email")

        every { mockedAuthService.userExists(any(String::class)) } returns null
        every {
            mockedAuthService.register(
                any(String::class),
                any(String::class)
            )
        } returns user

        val returned = authCommand.register(
            UserRegistrationRequest(
                "foo@example.com",
                "12345678",
                "12345678"
            )
        )

        assertThat(returned).isEqualTo(user)
    }

    @Test
    fun test_login_when_user_not_found() {
        every { mockedAuthService.verify(any(String::class), any(String::class)) } returns null

        assertThatThrownBy {
            authCommand.login(
                LoginRequest(
                    "foo@example.com",
                    "12345678"
                )
            )
        }.hasMessageContaining("user not found")
    }

    @Test
    fun test_login_when_password_is_bad() {
        every {
            mockedAuthService.verify(
                any(String::class),
                any(String::class)
            )
        } throws RuntimeException("bad password")

        assertThatThrownBy {
            authCommand.login(
                LoginRequest(
                    "foo@example.com",
                    "12345678"
                )
            )
        }.hasMessageContaining("bad password")
    }

    @Test
    fun test_login_when_token_building_fails() {
        every { mockedAuthService.verify(any(String::class), any(String::class)) } returns UUID.randomUUID()
        every {
            mockedJWTService.signJWT(any(), any(), any(), any())
        } throws RuntimeException("bad token")

        assertThatThrownBy {
            authCommand.login(
                LoginRequest(
                    "foo@example.com",
                    "12345678"
                )
            )
        }.hasMessageContaining("bad token")
    }

    @Test
    fun test_login_when_everything_works() {
        val generatedToken = UUID.randomUUID().toString()

        every { mockedAuthService.verify(any(String::class), any(String::class)) } returns UUID.randomUUID()
        every {
            mockedJWTService.signJWT(any(), any(), any(), any())
        } returns generatedToken

        val token =
            authCommand.login(
                LoginRequest(
                    "foo@example.com",
                    "12345678"
                )
            )

        assertThat(token).isEqualTo(generatedToken)
    }
}