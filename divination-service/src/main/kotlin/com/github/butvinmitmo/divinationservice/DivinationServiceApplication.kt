package com.github.butvinmitmo.divinationservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories

@SpringBootApplication(
    exclude = [
        JpaRepositoriesAutoConfiguration::class,
        HibernateJpaAutoConfiguration::class,
    ],
)
@EnableDiscoveryClient
@EnableFeignClients(basePackages = ["com.github.butvinmitmo.shared.client"])
@EnableR2dbcRepositories
class DivinationServiceApplication

fun main(args: Array<String>) {
    runApplication<DivinationServiceApplication>(*args)
}
