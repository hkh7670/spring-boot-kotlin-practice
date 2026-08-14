package com.example.springbootkotlinpractice.domain.order.event

import com.example.springbootkotlinpractice.common.Logging
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

// Kafka 연동이 실제로 동작하는지 확인하기 위한 로깅 전용 컨슈머.
// 알림 발송, 통계 집계 등 실제 후속 처리가 필요해지면 별도 컨슈머로 분리한다.
@Component
class OrderEventListener : Logging {

    @KafkaListener(topics = [OrderEventTopics.ORDER_PAID])
    fun onOrderPaid(event: OrderPaidEvent) {
        logger.info(
            "### [Kafka] order.paid 수신 orderId={} orderUid={} amount={}",
            event.orderId, event.orderUid, event.amount,
        )
    }

    @KafkaListener(topics = [OrderEventTopics.ORDER_CANCELLED])
    fun onOrderCancelled(event: OrderCancelledEvent) {
        logger.info(
            "### [Kafka] order.cancelled 수신 orderId={} orderUid={}",
            event.orderId, event.orderUid,
        )
    }
}
