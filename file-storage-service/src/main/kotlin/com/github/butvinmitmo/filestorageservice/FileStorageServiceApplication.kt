package com.github.butvinmitmo.filestorageservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient

@SpringBootApplication
@EnableDiscoveryClient
class FileStorageServiceApplication

fun main(args: Array<String>) {
    runApplication<FileStorageServiceApplication>(*args)
}
