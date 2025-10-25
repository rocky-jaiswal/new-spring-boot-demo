package dev.rockyj.example.demo.domain.entities

import dev.rockyj.example.demo.domain.dtos.UserDTO
import jakarta.persistence.*
import org.hibernate.Hibernate
import java.io.Serializable
import java.util.*

@Entity
@Table(name = "users")
class User : Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    lateinit var id: UUID

    @Column(name = "email", columnDefinition = "TEXT", nullable = false)
    var email: String? = null

    @Column(name = "password_hash", columnDefinition = "TEXT", nullable = false)
    var passwordHash: String? = null

    fun toDTO(): UserDTO {
        return UserDTO(this.id, this.email)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as User

        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
