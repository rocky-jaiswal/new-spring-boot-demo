package dev.rockyj.example.demo.domain.repositories

import dev.rockyj.example.demo.domain.entities.User
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository : CrudRepository<User, UUID> {

    fun findByEmail(email: String): User?

}
