package com.precisionfarming.security

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.core.task.support.TaskExecutorAdapter
import org.springframework.scheduling.annotation.EnableAsync
import java.util.concurrent.Executors

@Configuration
@EnableAsync
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
class VirtualThreadConfig {
    @Bean(name = ["applicationTaskExecutor"])
    fun applicationTaskExecutor(): AsyncTaskExecutor =
        TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor())
}
