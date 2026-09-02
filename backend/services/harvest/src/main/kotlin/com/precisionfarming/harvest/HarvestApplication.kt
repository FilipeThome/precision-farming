package com.precisionfarming.harvest

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(scanBasePackages = ["com.precisionfarming"])
@EnableScheduling
class HarvestApplication

fun main(args: Array<String>) {
    runApplication<HarvestApplication>(*args)
}
