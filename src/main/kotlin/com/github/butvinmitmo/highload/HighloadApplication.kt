package com.github.butvinmitmo.highload

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
class HighloadApplication

fun main(args: Array<String>) {
    runApplication<HighloadApplication>(*args)
}
