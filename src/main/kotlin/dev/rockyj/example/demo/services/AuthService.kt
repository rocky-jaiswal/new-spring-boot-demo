package dev.rockyj.example.demo.services

import dev.rockyj.example.demo.domain.dtos.UserDTO
import dev.rockyj.example.demo.domain.entities.User
import dev.rockyj.example.demo.domain.repositories.UserRepository
import org.springframework.stereotype.Service
import java.util.*

@Service
class AuthService(
    private val passwordService: PasswordService,
    private val userRepository: UserRepository,
) {
    fun userExists(email: String): UUID? {
        val record = userRepository.findByEmail(email)
        return record?.id
    }

    fun register(
        inputEmail: String,
        password: String,
    ): UserDTO {
        val user = User().apply {
            email = inputEmail
            passwordHash = passwordService.hashPassword(password)
        }

        return userRepository.save(user).toDTO()
    }

    fun verify(
        email: String,
        password: String,
    ): UUID? {
        val rec = userRepository.findByEmail(email.lowercase()) ?: return null
        val verified = passwordService.verifyPassword(password, rec.passwordHash!!)
        return if (verified) (rec.id) else throw RuntimeException("bad password")
    }
}