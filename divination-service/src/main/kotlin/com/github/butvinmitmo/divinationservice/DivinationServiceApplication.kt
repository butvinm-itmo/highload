package com.github.butvinmitmo.divinationservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients

@SpringBootApplication
@EnableFeignClients
class DivinationServiceApplication

fun main(args: Array<String>) {
    runApplication<DivinationServiceApplication>(*args)
}
