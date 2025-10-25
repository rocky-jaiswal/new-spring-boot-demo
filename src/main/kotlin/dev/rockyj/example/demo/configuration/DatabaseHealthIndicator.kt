package dev.rockyj.example.demo.configuration

import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import kotlin.system.measureTimeMillis

@Component
class DatabaseHealthIndicator(
    private val jdbcTemplate: JdbcTemplate
) : HealthIndicator {

    override fun health(): Health {
        return try {
            val response = jdbcTemplate.queryForObject("SELECT CURRENT_TIMESTAMP", OffsetDateTime::class.java)

            Health.up()
                .withDetail("timestamp", "${response.toString()}ms")
                .build()

        } catch (e: Exception) {
            Health.down()
                .withException(e)
                .withDetail("error", "Database query failed: ${e.message}")
                .build()
        }
    }
}