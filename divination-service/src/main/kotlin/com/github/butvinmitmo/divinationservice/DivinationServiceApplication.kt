package com.github.butvinmitmo.divinationservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.cloud.openfeign.EnableFeignClients

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = ["com.github.butvinmitmo.shared.client"])
class DivinationServiceApplication

fun main(args: Array<String>) {
    runApplication<DivinationServiceApplication>(*args)
}
