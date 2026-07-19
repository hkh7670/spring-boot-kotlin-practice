package com.example.springbootkotlinpractice.domain.payment.service

import com.example.springbootkotlinpractice.common.payment.toss.TossConfirmPaymentRequest
import com.example.springbootkotlinpractice.common.payment.toss.TossConfirmPaymentResponse
import com.example.springbootkotlinpractice.common.payment.toss.TossPaymentsApi
import com.example.springbootkotlinpractice.domain.delivery.repository.DeliveryInfoRepository
import com.example.springbootkotlinpractice.domain.order.entity.OrderInfo
import com.example.springbootkotlinpractice.domain.order.repository.OrderDetailInfoRepository
import com.example.springbootkotlinpractice.domain.order.repository.OrderInfoRepository
import com.example.springbootkotlinpractice.domain.payment.dto.PaymentConfirmRequest
import com.example.springbootkotlinpractice.domain.payment.dto.PaymentConfirmResponse
import com.example.springbootkotlinpractice.domain.payment.entity.PayInfo
import com.example.springbootkotlinpractice.domain.payment.repository.PayInfoRepository
import com.example.springbootkotlinpractice.domain.product.repository.ProductInfoRepository
import com.example.springbootkotlinpractice.enums.OrderStatus
import com.example.springbootkotlinpractice.enums.PaymentStatus
import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.exception.ApiErrorException
import java.time.OffsetDateTime
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PaymentService(
    private val orderInfoRepository: OrderInfoRepository,
    private val orderDetailInfoRepository: OrderDetailInfoRepository,
    private val payInfoRepository: PayInfoRepository,
    private val deliveryInfoRepository: DeliveryInfoRepository,
    private val productInfoRepository: ProductInfoRepository,
    private val tossPaymentsApi: TossPaymentsApi,
) {

    // Toss 승인이 명확히 거절되면 주문을 취소하고 재고를 복구한 뒤 예외를 던진다.
    // noRollbackFor 가 없으면 그 예외 때문에 취소/재고복구 변경사항까지 함께 롤백되어 버린다.
    @Transactional(noRollbackFor = [ApiErrorException::class])
    fun confirmPayment(memberId: Long, request: PaymentConfirmRequest): PaymentConfirmResponse {
        val order = getOrder(request.orderId)
        validateOwner(order, memberId)
        validateOrderStatus(order)

        val expectedAmount = calculateExpectedAmount(order)
        validateAmount(expectedAmount, request.amount)

        val tossResponse = confirmToss(order, request)
        validateAmount(expectedAmount, tossResponse.totalAmount)

        order.markPaid()
        val payInfo = payInfoRepository.save(
            PayInfo.of(
                orderId = order.id,
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

    private fun getOrder(orderUid: String): OrderInfo {
        return orderInfoRepository.findByOrderUid(orderUid)
            ?: throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_ORDER)
    }

    private fun validateOwner(order: OrderInfo, memberId: Long) {
        if (order.memberId != memberId) {
            throw ApiErrorException(ResponseCodeEnum.ACCESS_DENIED)
        }
    }

    private fun validateOrderStatus(order: OrderInfo) {
        when (order.status) {
            OrderStatus.PAID -> throw ApiErrorException(ResponseCodeEnum.ALREADY_PAID_ORDER)
            OrderStatus.CANCELLED -> throw ApiErrorException(ResponseCodeEnum.ORDER_ALREADY_CANCELLED)
            OrderStatus.PENDING_PAYMENT -> Unit
        }
    }

    private fun calculateExpectedAmount(order: OrderInfo): Int {
        val deliveryInfo = deliveryInfoRepository.findByIdOrNull(order.deliveryInfoId)
            ?: throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_DELIVERY)
        return order.productTotalPrice + deliveryInfo.price
    }

    private fun validateAmount(expectedAmount: Int, actualAmount: Int) {
        if (expectedAmount != actualAmount) {
            throw ApiErrorException(ResponseCodeEnum.PAYMENT_AMOUNT_MISMATCH)
        }
    }

    private fun confirmToss(order: OrderInfo, request: PaymentConfirmRequest): TossConfirmPaymentResponse {
        return runCatching {
            tossPaymentsApi.confirmPayment(
                TossConfirmPaymentRequest(
                    paymentKey = request.paymentKey,
                    orderId = request.orderId,
                    amount = request.amount,
                )
            )
        }.getOrElse {
            cancelOrderAndRestoreStock(order)
            if (it is ApiErrorException) {
                throw it
            }
            throw ApiErrorException(ResponseCodeEnum.PAYMENT_CONFIRM_FAILED)
        }
    }

    private fun cancelOrderAndRestoreStock(order: OrderInfo) {
        order.markCancelled()
        orderDetailInfoRepository.findByOrderId(order.id).forEach {
            productInfoRepository.increaseStock(it.productId, it.count)
        }
    }
}
