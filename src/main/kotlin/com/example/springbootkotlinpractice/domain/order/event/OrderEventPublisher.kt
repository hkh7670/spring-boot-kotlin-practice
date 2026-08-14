package com.example.springbootkotlinpractice.domain.order.event

import com.example.springbootkotlinpractice.common.Logging
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

// Kafka 브로커로의 실제 발행만 담당한다. DB 트랜잭션 커밋 이후 시점에만 호출되어야 하며,
// 그 책임은 OrderEventRelay(@TransactionalEventListener)가 진다.
@Component
class OrderEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
) : Logging {

    fun publish(event: OrderPaidEvent) {
        send(OrderEventTopics.ORDER_PAID, event.orderUid, event)
    }

    fun publish(event: OrderCancelledEvent) {
        send(OrderEventTopics.ORDER_CANCELLED, event.orderUid, event)
    }

    private fun send(topic: String, key: String, payload: Any) {
        kafkaTemplate.send(topic, key, payload).whenComplete { _, ex ->
            if (ex != null) {
                logger.error("### Kafka 이벤트 발행 실패 topic={} key={}", topic, key, ex)
            }
        }
    }
}
