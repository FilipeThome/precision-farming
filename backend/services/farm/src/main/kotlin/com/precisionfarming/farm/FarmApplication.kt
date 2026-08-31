package com.precisionfarming.farm

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(scanBasePackages = ["com.precisionfarming"])
@EnableScheduling
class FarmApplication

fun main(args: Array<String>) {
    runApplication<FarmApplication>(*args)
}
