package com.precisionfarming.file

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(scanBasePackages = ["com.precisionfarming"])
@EnableScheduling
class FileApplication

fun main(args: Array<String>) {
    runApplication<FileApplication>(*args)
}
