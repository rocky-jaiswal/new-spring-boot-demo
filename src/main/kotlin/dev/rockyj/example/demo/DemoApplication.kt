package dev.rockyj.example.demo

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.env.PropertiesPropertySourceLoader
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component

@SpringBootApplication
class DemoApplication

fun main(args: Array<String>) {
    SpringApplication(DemoApplication::class.java).apply {
        addInitializers(SecretsInitializer())
    }.run(*args)
}

@Component
class SecretsInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {

    private val loader = PropertiesPropertySourceLoader()

    override fun initialize(context: ConfigurableApplicationContext) {
        val profile = System.getenv("SPRING_PROFILES_ACTIVE") ?: "dev"
        val path = ClassPathResource(".env.${profile}")
        val propertySource = loader.load("secrets-resource", path).first()
        context.environment.propertySources.addFirst(propertySource)
    }
}
