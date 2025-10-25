package dev.rockyj.example.demo.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class Secrets(
    @Value("\${spring.myapp.privateKeyPath}")
    val privateKeyPath: String,
    @Value("\${spring.myapp.publicKeyPath}")
    val publicKeyPath: String,
    @Value("\${spring.myapp.keyPassword}")
    val privateKeyPassword: String) {
}