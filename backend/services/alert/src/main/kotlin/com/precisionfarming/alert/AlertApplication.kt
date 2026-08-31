package com.precisionfarming.alert

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(scanBasePackages = ["com.precisionfarming"])
@EnableScheduling
class AlertApplication

fun main(args: Array<String>) {
    runApplication<AlertApplication>(*args)
}
