package com.precisionfarming.telemetry

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(scanBasePackages = ["com.precisionfarming"])
@EnableScheduling
class TelemetryApplication

fun main(args: Array<String>) {
    runApplication<TelemetryApplication>(*args)
}
