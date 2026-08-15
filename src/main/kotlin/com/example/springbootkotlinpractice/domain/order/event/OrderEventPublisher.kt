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

    // kafkaTemplate.send() 는 비동기 실패는 반환된 Future 로 알리지만, 브로커 메타데이터를
    // max.block.ms 안에 못 받아오면 send() 호출 자체에서 동기적으로 예외를 던진다. 이벤트 발행은
    // 어디까지나 부가 기능이라 그 실패가 호출자(주문 취소 등 핵심 로직)까지 전파돼서는 안 되므로
    // 두 경로 모두 여기서 흡수한다.
    private fun send(topic: String, key: String, payload: Any) {
        runCatching {
            kafkaTemplate.send(topic, key, payload).whenComplete { _, ex ->
                if (ex != null) {
                    logger.error("### Kafka 이벤트 발행 실패(비동기) topic={} key={}", topic, key, ex)
                }
            }
        }.onFailure {
            logger.error("### Kafka 이벤트 발행 실패(동기) topic={} key={}", topic, key, it)
        }
    }
}
