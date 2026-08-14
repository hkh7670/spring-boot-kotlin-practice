package com.example.springbootkotlinpractice.domain.order.event

import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

// PaymentRecordService 의 @Transactional 메서드 안에서 ApplicationEventPublisher 로 발행된 이벤트를
// 받아, 그 트랜잭션이 커밋에 성공한 이후에만 Kafka로 전달한다. 커밋 전에 바로 Kafka로 보내면
// 이후 트랜잭션이 롤백되더라도 이미 나간 이벤트를 취소할 수 없기 때문이다.
@Component
class OrderEventRelay(
    private val orderEventPublisher: OrderEventPublisher,
) {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onOrderPaid(event: OrderPaidEvent) {
        orderEventPublisher.publish(event)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onOrderCancelled(event: OrderCancelledEvent) {
        orderEventPublisher.publish(event)
    }
}
