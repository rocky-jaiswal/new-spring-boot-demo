package dev.rockyj.example.demo.controllers.requests

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [PasswordMatchValidator::class])
annotation class PasswordMatch(
    val message: String = "Passwords do not match",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class PasswordMatchValidator : ConstraintValidator<PasswordMatch, UserRegistrationRequest> {

    override fun isValid(value: UserRegistrationRequest?, context: ConstraintValidatorContext): Boolean {
        if (value == null) return true

        val isValid = value.password == value.confirmedPassword

        if (!isValid) {
            // Customize which field the error is associated with
            context.disableDefaultConstraintViolation()
            context.buildConstraintViolationWithTemplate(context.defaultConstraintMessageTemplate)
                .addPropertyNode("confirmedPassword")
                .addConstraintViolation()
        }

        return isValid
    }
}

@PasswordMatch
data class UserRegistrationRequest(
    @field:NotBlank
    val email: String,

    @field:NotBlank
    @field:Size(min = 8)
    val password: String,

    @field:NotBlank
    @field:Size(min = 8)
    val confirmedPassword: String
)
