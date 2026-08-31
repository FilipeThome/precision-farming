package com.precisionfarming.integration

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(scanBasePackages = ["com.precisionfarming"])
@EnableScheduling
class IntegrationApplication

fun main(args: Array<String>) {
    runApplication<IntegrationApplication>(*args)
}
