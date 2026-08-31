package com.precisionfarming.operation

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(scanBasePackages = ["com.precisionfarming"])
@EnableScheduling
class OperationApplication

fun main(args: Array<String>) {
    runApplication<OperationApplication>(*args)
}
