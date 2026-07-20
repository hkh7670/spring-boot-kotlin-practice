package com.example.springbootkotlinpractice.domain.payment.service

import com.example.springbootkotlinpractice.common.payment.toss.TossConfirmPaymentResponse
import com.example.springbootkotlinpractice.domain.order.entity.OrderStatusHistory
import com.example.springbootkotlinpractice.domain.order.repository.OrderDetailInfoRepository
import com.example.springbootkotlinpractice.domain.order.repository.OrderInfoRepository
import com.example.springbootkotlinpractice.domain.order.repository.OrderStatusHistoryRepository
import com.example.springbootkotlinpractice.domain.payment.dto.PaymentConfirmResponse
import com.example.springbootkotlinpractice.domain.payment.entity.PayInfo
import com.example.springbootkotlinpractice.domain.payment.repository.PayInfoRepository
import com.example.springbootkotlinpractice.domain.product.repository.ProductInfoRepository
import com.example.springbootkotlinpractice.enums.PaymentStatus
import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.exception.ApiErrorException
import java.time.OffsetDateTime
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// Toss API 호출과 분리된, 결제 확정/취소 결과를 DB에 기록하는 책임만 담당한다.
// 각 메서드가 별도 트랜잭션이라 Toss 호출(느릴 수 있는 외부 네트워크 요청) 동안 DB 커넥션을 붙잡고 있지 않는다.
@Service
class PaymentRecordService(
    private val orderInfoRepository: OrderInfoRepository,
    private val orderDetailInfoRepository: OrderDetailInfoRepository,
    private val orderStatusHistoryRepository: OrderStatusHistoryRepository,
    private val payInfoRepository: PayInfoRepository,
    private val productInfoRepository: ProductInfoRepository,
) {

    @Transactional
    fun completePayment(orderId: Long, tossResponse: TossConfirmPaymentResponse): PaymentConfirmResponse {
        val order = orderInfoRepository.findByIdOrNull(orderId)
            ?: throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_ORDER)

        order.markPaid()
        orderStatusHistoryRepository.save(OrderStatusHistory.of(order.id, order.status))

        val payInfo = payInfoRepository.save(
            PayInfo.of(
                orderInfoId = order.id,
                paymentKey = tossResponse.paymentKey,
                amount = tossResponse.totalAmount,
                status = PaymentStatus.valueOf(tossResponse.status),
                method = tossResponse.method,
                approvedAt = tossResponse.approvedAt?.let { OffsetDateTime.parse(it).toLocalDateTime() },
            )
        )

        return PaymentConfirmResponse(
            payInfoId = payInfo.id,
            orderId = order.id,
            orderUid = order.orderUid,
            paymentKey = payInfo.paymentKey,
            amount = payInfo.amount,
            status = payInfo.status,
            method = payInfo.method,
            approvedAt = payInfo.approvedAt,
        )
    }

    @Transactional
    fun cancelOrderAndRestoreStock(orderId: Long) {
        val orderInfo = orderInfoRepository.findByIdOrNull(orderId)
            ?: throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_ORDER)

        orderInfo.markCancelled()
        orderStatusHistoryRepository.save(OrderStatusHistory.of(orderInfo.id, orderInfo.status))

        orderDetailInfoRepository.findByOrderInfo(orderInfo).forEach {
            productInfoRepository.increaseStock(it.productInfo.id, it.count)
        }
    }
}
