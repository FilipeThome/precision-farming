package com.precisionfarming.sync

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(scanBasePackages = ["com.precisionfarming"])
@EnableScheduling
class SyncApplication

fun main(args: Array<String>) {
    runApplication<SyncApplication>(*args)
}
