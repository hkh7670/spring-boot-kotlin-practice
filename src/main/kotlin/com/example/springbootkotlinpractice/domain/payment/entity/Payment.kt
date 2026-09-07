package com.example.springbootkotlinpractice.domain.payment.entity

import com.example.springbootkotlinpractice.common.entity.BaseTimeEntity
import com.example.springbootkotlinpractice.enums.PaymentStatus
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "payments",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_payments_01", columnNames = ["order_id"]),
        UniqueConstraint(name = "uq_payments_02", columnNames = ["payment_key"]),
    ],
    comment = "결제 정보",
)
class Payment(

    @Column(name = "order_id", nullable = false, updatable = false, comment = "주문 ID (orders.id)")
    val orderId: Long,

    @Column(
        name = "payment_key", nullable = false, updatable = false, length = 200,
        comment = "Toss Payments 결제 고유 키",
    )
    val paymentKey: String,

    @Column(name = "amount", nullable = false, updatable = false, comment = "결제 금액")
    val amount: Int,

    // 이 필드는 항상 cancel()/done() 같은 이름 있는 메서드를 통해서만 변경한다 (status = X 직접 대입 금지).
    // Order.status 와 같은 이유로 컴파일 타임 강제(private/protected set)는 적용하지 않는다.
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20, comment = "결제 상태")
    var status: PaymentStatus,

    @Column(name = "method", nullable = true, length = 30, comment = "결제 수단 (카드, 가상계좌 등)")
    val method: String? = null,

    @Column(name = "approved_at", nullable = true, comment = "결제 승인 일시")
    val approvedAt: LocalDateTime? = null,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0L

    companion object {
        fun of(
            orderId: Long,
            paymentKey: String,
            amount: Int,
            status: PaymentStatus,
            method: String?,
            approvedAt: LocalDateTime?,
        ): Payment {
            return Payment(
                orderId = orderId,
                paymentKey = paymentKey,
                amount = amount,
                status = status,
                method = method,
                approvedAt = approvedAt,
            )
        }
    }

    fun cancel() {
        status = PaymentStatus.CANCELED
    }

    fun done() {
        status = PaymentStatus.DONE
    }
}
