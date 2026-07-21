package com.example.springbootkotlinpractice.domain.order.scheduler

import com.example.springbootkotlinpractice.common.Logging
import com.example.springbootkotlinpractice.domain.order.repository.OrderRepository
import com.example.springbootkotlinpractice.domain.payment.service.PaymentRecordService
import com.example.springbootkotlinpractice.enums.OrderStatus
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

// 결제 대기(PENDING_PAYMENT) 상태로 일정 시간이 지난 주문은 결제 포기로 간주하고 취소 + 재고 복구한다.
@Component
class StaleOrderCancelScheduler(
    private val orderRepository: OrderRepository,
    private val paymentRecordService: PaymentRecordService,
) : Logging {

    companion object {
        private const val STALE_ORDER_THRESHOLD_MINUTES = 10L
    }

    @Scheduled(cron = "0 */10 * * * *")
    fun cancelStaleOrders() {
        logger.info("### 결제 대기 만료 주문 취소 처리 배치 시작 ###")
        val orderList = orderRepository.findByStatusAndCreatedDatetimeBefore(
            OrderStatus.PENDING_PAYMENT,
            LocalDateTime.now().minusMinutes(STALE_ORDER_THRESHOLD_MINUTES),
        )

        orderList.forEach { order ->
            runCatching {
                logger.info("### 결제 대기 만료 주문 취소 처리 (orderId: {})", order.id)
                paymentRecordService.cancelOrderAndRestoreStock(order.id)
            }.onFailure {
                logger.error("### 결제 대기 만료 주문 취소 처리에 실패했습니다. orderId={}", order.id, it)
            }
        }
    }

}
