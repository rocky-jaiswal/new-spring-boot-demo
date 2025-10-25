package dev.rockyj.example.demo.controllers

import org.slf4j.LoggerFactory
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@Validated
@RestController
@RequestMapping("/api/v1/todos")
class TodosController {

    private val log = LoggerFactory.getLogger(TodosController::class.java)

    @PostMapping("/")
    fun create(): Map<String, String> {
        return emptyMap()
    }
}