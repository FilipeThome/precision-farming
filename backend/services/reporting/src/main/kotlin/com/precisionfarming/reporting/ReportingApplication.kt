package com.precisionfarming.reporting

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(scanBasePackages = ["com.precisionfarming"])
@EnableScheduling
class ReportingApplication

fun main(args: Array<String>) {
    runApplication<ReportingApplication>(*args)
}
