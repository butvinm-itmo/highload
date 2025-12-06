package com.github.butvinmitmo.e2e

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.openfeign.EnableFeignClients

@SpringBootApplication
@EnableFeignClients(basePackages = ["com.github.butvinmitmo.e2e.client"])
class E2ETestApplication
