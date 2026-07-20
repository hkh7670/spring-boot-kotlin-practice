package com.example.springbootkotlinpractice.domain.order.entity

import com.example.springbootkotlinpractice.common.entity.BaseTimeEntity
import com.example.springbootkotlinpractice.enums.OrderStatus
import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.exception.ApiErrorException
import com.github.f4b6a3.ulid.UlidCreator
import jakarta.persistence.*
import org.hibernate.annotations.Comment

@Entity
@Table(
    name = "order_info",
    uniqueConstraints = [
        UniqueConstraint(name = "order_info_unique_1", columnNames = ["order_uid"]),
    ]
)
@Comment("주문 정보")
class OrderInfo(

    @Comment("외부 노출용 주문 식별자 (ULID, Toss orderId)")
    @Column(name = "order_uid", nullable = false, updatable = false, length = 26)
    val orderUid: String = UlidCreator.getUlid().toString(),

    @Comment("주문한 유저의 ID (member.id)")
    @Column(name = "member_id", nullable = false, updatable = false)
    val memberId: Long,

    @Comment("상품 전체 가격")
    @Column(name = "product_total_price", nullable = false)
    var productTotalPrice: Int = 0,

    @Comment("배송 정보 ID (delivery_info.id)")
    @Column(name = "delivery_info_id", nullable = false, updatable = false)
    val deliveryInfoId: Long,

    @Comment("주문 시점의 배송 가격 (delivery_info.price 는 이후 변경될 수 있어 스냅샷 저장)")
    @Column(name = "delivery_price", nullable = false, updatable = false)
    val deliveryPrice: Int = 0,

    @Comment("주문 상태")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    var status: OrderStatus = OrderStatus.PENDING_PAYMENT,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0L

    fun markPaid() {
        validatePendingPayment()
        status = OrderStatus.PAID
    }

    fun markCancelled() {
        validatePendingPayment()
        status = OrderStatus.CANCELLED
    }

    private fun validatePendingPayment() {
        when (status) {
            OrderStatus.PAID -> throw ApiErrorException(ResponseCodeEnum.ALREADY_PAID_ORDER)
            OrderStatus.CANCELLED -> throw ApiErrorException(ResponseCodeEnum.ORDER_ALREADY_CANCELLED)
            OrderStatus.PENDING_PAYMENT -> Unit
        }
    }

    companion object {
        fun of(
            memberId: Long,
            productTotalPrice: Int,
            deliveryInfoId: Long,
            deliveryPrice: Int
        ): OrderInfo {
            return OrderInfo(
                memberId = memberId,
                productTotalPrice = productTotalPrice,
                deliveryInfoId = deliveryInfoId,
                deliveryPrice = deliveryPrice,
            )
        }
    }
}
