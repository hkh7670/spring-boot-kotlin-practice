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
    name = "orders",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_orders_01", columnNames = ["order_uid"]),
    ]
)
@Comment("주문 정보")
class Order(

    @Comment("외부 노출용 주문 식별자 (ULID, Toss orderId)")
    @Column(name = "order_uid", nullable = false, updatable = false, length = 26)
    val orderUid: String = UlidCreator.getUlid().toString(),

    @Comment("주문한 유저의 ID (member.id)")
    @Column(name = "member_id", nullable = false, updatable = false)
    val memberId: Long,

    @Comment("상품 전체 가격")
    @Column(name = "product_total_price", nullable = false)
    var productTotalPrice: Int = 0,

    @Comment("배송 옵션 정보 ID (delivery_options.id)")
    @Column(name = "delivery_option_id", nullable = false, updatable = false)
    val deliveryOptionId: Long,

    @Comment("주문 시점의 배송 가격 (delivery_info.price 는 이후 변경될 수 있어 스냅샷 저장)")
    @Column(name = "delivery_price", nullable = false, updatable = false)
    val deliveryPrice: Int = 0,

    // Kotlin 문법상 주 생성자 프로퍼티에는 접근자(private/protected set)를 붙일 수 없다. 컴파일 타임으로
    // 강제하진 않되, 이 필드는 항상 markPaid()/markCancelled()/cancelPaidOrder() 같은 이름 있는 메서드를
    // 통해서만 변경한다 (status = X 직접 대입 금지).
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

    // 결제 완료(PAID) 주문의 취소(환불) 전용. markCancelled() 는 결제 전(PENDING_PAYMENT) 주문 전용이라
    // 재사용할 수 없다 (PAID 상태에서 호출하면 ALREADY_PAID_ORDER 를 던진다). 배송이 시작된(SHIPPING)
    // 이후로는 취소를 막는다.
    fun cancelPaidOrder() {
        when (status) {
            OrderStatus.PAID -> status = OrderStatus.CANCELLED
            OrderStatus.CANCELLED -> throw ApiErrorException(ResponseCodeEnum.ORDER_ALREADY_CANCELLED)
            OrderStatus.PENDING_PAYMENT -> throw ApiErrorException(ResponseCodeEnum.ORDER_NOT_PAID)
            OrderStatus.SHIPPING, OrderStatus.DELIVERED,
            OrderStatus.RETURNING, OrderStatus.RETURNED -> throw ApiErrorException(ResponseCodeEnum.ORDER_ALREADY_SHIPPING)
        }
    }

    // 운영자가 배송을 시작 처리한다.
    fun markShipping() {
        when (status) {
            OrderStatus.PAID -> status = OrderStatus.SHIPPING
            else -> throw ApiErrorException(ResponseCodeEnum.ORDER_NOT_SHIPPABLE)
        }
    }

    // 운영자가 배송완료 처리한다.
    fun markDelivered() {
        when (status) {
            OrderStatus.SHIPPING -> status = OrderStatus.DELIVERED
            else -> throw ApiErrorException(ResponseCodeEnum.ORDER_NOT_SHIPPING)
        }
    }

    // 고객이 배송완료된 주문의 반품을 요청한다.
    fun requestReturn() {
        when (status) {
            OrderStatus.DELIVERED -> status = OrderStatus.RETURNING
            else -> throw ApiErrorException(ResponseCodeEnum.ORDER_NOT_DELIVERED)
        }
    }

    // 반품 택배가 도착했을 때 운영자가 반품완료 처리한다.
    fun completeReturn() {
        when (status) {
            OrderStatus.RETURNING -> status = OrderStatus.RETURNED
            else -> throw ApiErrorException(ResponseCodeEnum.ORDER_NOT_RETURNING)
        }
    }

    private fun validatePendingPayment() {
        when (status) {
            OrderStatus.PAID -> throw ApiErrorException(ResponseCodeEnum.ALREADY_PAID_ORDER)
            OrderStatus.CANCELLED -> throw ApiErrorException(ResponseCodeEnum.ORDER_ALREADY_CANCELLED)
            OrderStatus.PENDING_PAYMENT -> Unit
            OrderStatus.SHIPPING, OrderStatus.DELIVERED,
            OrderStatus.RETURNING, OrderStatus.RETURNED -> throw ApiErrorException(ResponseCodeEnum.ALREADY_PAID_ORDER)
        }
    }

    companion object {
        fun of(
            memberId: Long,
            productTotalPrice: Int,
            deliveryOptionId: Long,
            deliveryPrice: Int
        ): Order {
            return Order(
                memberId = memberId,
                productTotalPrice = productTotalPrice,
                deliveryOptionId = deliveryOptionId,
                deliveryPrice = deliveryPrice,
            )
        }
    }
}
