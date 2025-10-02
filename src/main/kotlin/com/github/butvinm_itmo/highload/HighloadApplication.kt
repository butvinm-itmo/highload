package com.github.butvinm_itmo.highload

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
class HighloadApplication

@RestController
class BlogController {
  @GetMapping("/")
  fun blog(): String {
    return "blog"
  }
}

fun main(args: Array<String>) {
	runApplication<HighloadApplication>(*args)
}
