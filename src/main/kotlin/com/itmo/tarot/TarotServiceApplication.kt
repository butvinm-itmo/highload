package com.itmo.tarot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TarotServiceApplication

fun main(args: Array<String>) {
    runApplication<TarotServiceApplication>(*args)
}