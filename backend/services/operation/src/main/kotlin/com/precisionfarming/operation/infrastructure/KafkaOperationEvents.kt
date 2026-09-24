package com.precisionfarming.operation.infrastructure

import com.precisionfarming.operation.application.OperationEvents
import com.precisionfarming.operation.application.operationStartedJson
import jakarta.annotation.PreDestroy
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import java.util.Properties
import java.util.UUID

class KafkaOperationEvents(
    bootstrapServers: String,
    private val producer: KafkaProducer<String, String> = createProducer(bootstrapServers),
) : OperationEvents {

    override fun operationStarted(operationId: UUID, farmId: UUID, fieldId: UUID, prescriptionId: UUID?) {
        val payload = operationStartedJson(operationId, farmId, fieldId, prescriptionId)
        producer.send(ProducerRecord(TOPIC, operationId.toString(), payload)) { _, ex ->
            if (ex != null) {
                log.warn("Kafka publish failed for {}: {}", TOPIC, ex.message)
            }
        }
    }

    @PreDestroy
    fun close() {
        producer.close()
    }

    companion object {
        const val TOPIC = "precision.operation.started"
        private val log = LoggerFactory.getLogger(KafkaOperationEvents::class.java)

        private fun createProducer(bootstrapServers: String): KafkaProducer<String, String> {
            val props = Properties().apply {
                put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
                put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
                put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
                put(ProducerConfig.ACKS_CONFIG, "1")
                put(ProducerConfig.MAX_BLOCK_MS_CONFIG, "500")
                put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, "3000")
                put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, "2000")
                put(ProducerConfig.LINGER_MS_CONFIG, "0")
            }
            return KafkaProducer(props)
        }
    }
}
