package com.precisionfarming.irrigation

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(scanBasePackages = ["com.precisionfarming"])
@EnableScheduling
class IrrigationApplication

fun main(args: Array<String>) {
    runApplication<IrrigationApplication>(*args)
}
