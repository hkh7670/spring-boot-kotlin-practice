package com.example.springbootkotlinpractice.common.payment.toss

import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.exception.ApiErrorException
import org.springframework.stereotype.Component

// Toss 결제취소 API 호출 + 표준 에러 처리를 한 곳에 모은다.
// 주문취소(OrderCancelService)와 반품완료(OrderReturnService) 양쪽에서 재사용한다.
@Component
class TossPaymentCanceller(
    private val tossPaymentsApi: TossPaymentsApi,
) {

    fun cancel(paymentKey: String, cancelReason: String) {
        runCatching {
            tossPaymentsApi.cancelPayment(
                paymentKey,
                TossCancelPaymentRequest(cancelReason = cancelReason)
            )
        }.onFailure {
            if (it is ApiErrorException) {
                throw it
            }
            throw ApiErrorException(ResponseCodeEnum.PAYMENT_CANCEL_FAILED)
        }
    }
}
