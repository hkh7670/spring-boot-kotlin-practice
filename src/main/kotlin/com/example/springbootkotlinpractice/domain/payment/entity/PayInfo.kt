package com.example.springbootkotlinpractice.domain.payment.entity

import com.example.springbootkotlinpractice.common.entity.BaseTimeEntity
import com.example.springbootkotlinpractice.enums.PaymentStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime
import org.hibernate.annotations.Comment

@Entity
@Table(
    name = "pay_info",
    uniqueConstraints = [
        UniqueConstraint(name = "pay_info_unique_1", columnNames = ["order_id"]),
        UniqueConstraint(name = "pay_info_unique_2", columnNames = ["payment_key"]),
    ]
)
@Comment("결제 정보")
class PayInfo(

    @Comment("주문 ID (order_info.id)")
    @Column(name = "order_id", nullable = false, updatable = false)
    val orderId: Long,

    @Comment("Toss Payments 결제 고유 키")
    @Column(name = "payment_key", nullable = false, updatable = false, length = 200)
    val paymentKey: String,

    @Comment("결제 금액")
    @Column(name = "amount", nullable = false, updatable = false)
    val amount: Int,

    @Comment("결제 상태")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: PaymentStatus,

    @Comment("결제 수단 (카드, 가상계좌 등)")
    @Column(name = "method", nullable = true, length = 30)
    val method: String? = null,

    @Comment("결제 승인 일시")
    @Column(name = "approved_at", nullable = true)
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
        ): PayInfo {
            return PayInfo(
                orderId = orderId,
                paymentKey = paymentKey,
                amount = amount,
                status = status,
                method = method,
                approvedAt = approvedAt,
            )
        }
    }
}
