package com.github.butvinmitmo.tarotservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient

@SpringBootApplication
@EnableDiscoveryClient
class TarotServiceApplication

fun main(args: Array<String>) {
    runApplication<TarotServiceApplication>(*args)
}
