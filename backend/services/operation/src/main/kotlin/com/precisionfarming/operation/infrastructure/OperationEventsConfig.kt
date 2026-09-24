package com.precisionfarming.operation.infrastructure

import com.precisionfarming.operation.application.NoOpOperationEvents
import com.precisionfarming.operation.application.OperationEvents
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OperationEventsConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.kafka", name = ["enabled"], havingValue = "true")
    fun kafkaOperationEvents(
        @Value("\${app.kafka.bootstrap-servers:127.0.0.1:9092}") bootstrapServers: String,
    ): OperationEvents = KafkaOperationEvents(bootstrapServers)

    @Bean
    @ConditionalOnProperty(
        prefix = "app.kafka",
        name = ["enabled"],
        havingValue = "false",
        matchIfMissing = true,
    )
    fun noOpOperationEvents(): OperationEvents = NoOpOperationEvents
}
