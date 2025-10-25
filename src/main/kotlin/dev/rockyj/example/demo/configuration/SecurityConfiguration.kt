package dev.rockyj.example.demo.configuration

import dev.rockyj.example.demo.services.JWTService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter
import org.springframework.stereotype.Service
import org.springframework.web.filter.OncePerRequestFilter

@Configuration
@EnableWebSecurity
class SecurityConfiguration {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { authorize ->
                authorize
                    //.requestMatchers("/public/**", "/health").permitAll()
                    //.requestMatchers("/api/v1/**").authenticated()
                    .anyRequest().permitAll()
            }.addFilterAfter (AuthHeaderFilter(), AnonymousAuthenticationFilter::class.java)

        http.csrf { it.disable() }
        http.formLogin { it.disable() }

        return http.build()
    }
}

@Service
class AuthHeaderFilter : OncePerRequestFilter() {

    @Autowired
    private lateinit var jwtService: JWTService

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (request.requestURI.startsWith("/api/v1") && !request.requestURI.startsWith("/api/v1/auth")) {
            val authHeader = request.getHeader("Authentication")

            if (authHeader.isNullOrBlank()) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing Authentication header")
                return
            }

            try {
                jwtService.verifyJWT(authHeader.removePrefix("Bearer").trim())
            } catch (ex: Exception) {
                // logger.error("Error in auth middleware", ex)
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Bad Authentication header")
                return
            }
        }

        filterChain.doFilter(request, response)
    }
}