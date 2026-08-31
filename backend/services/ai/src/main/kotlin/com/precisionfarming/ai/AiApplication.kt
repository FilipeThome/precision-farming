package com.precisionfarming.ai

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(scanBasePackages = ["com.precisionfarming"])
@EnableScheduling
class AiApplication

fun main(args: Array<String>) {
    runApplication<AiApplication>(*args)
}
