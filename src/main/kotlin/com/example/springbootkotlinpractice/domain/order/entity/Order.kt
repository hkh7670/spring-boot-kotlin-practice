package com.example.springbootkotlinpractice.domain.order.entity

import com.example.springbootkotlinpractice.common.entity.BaseTimeEntity
import com.example.springbootkotlinpractice.enums.OrderStatus
import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.exception.ApiErrorException
import com.github.f4b6a3.ulid.UlidCreator
import jakarta.persistence.*

@Entity
@Table(
    name = "orders",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_orders_01", columnNames = ["order_uid"]),
    ],
    comment = "주문 정보",
)
class Order(

    @Column(
        name = "order_uid", nullable = false, updatable = false, length = 26,
        comment = "외부 노출용 주문 식별자 (ULID, Toss orderId)",
    )
    val orderUid: String = UlidCreator.getUlid().toString(),

    @Column(name = "member_id", nullable = false, updatable = false, comment = "주문한 유저의 ID (member.id)")
    val memberId: Long,

    @Column(name = "product_total_price", nullable = false, comment = "상품 전체 가격")
    var productTotalPrice: Int = 0,

    @Column(
        name = "delivery_option_id", nullable = false, updatable = false,
        comment = "배송 옵션 정보 ID (delivery_options.id)",
    )
    val deliveryOptionId: Long,

    @Column(
        name = "delivery_price", nullable = false, updatable = false,
        comment = "주문 시점의 배송 가격 (delivery_info.price 는 이후 변경될 수 있어 스냅샷 저장)",
    )
    val deliveryPrice: Int = 0,

    // 주문 생성 트랜잭션 안에서 쿠폰/포인트 확정 후 applyDiscount()로 채워진다(주문 저장 시점엔
    // orderId가 없어 쿠폰/포인트 사용 확정을 먼저 할 수 없기 때문 — OrderService.createOrder() 참고)
    @Column(name = "coupon_discount_price", nullable = false, comment = "쿠폰으로 할인된 금액 (미사용 시 0)")
    var couponDiscountPrice: Int = 0,

    @Column(name = "point_discount_price", nullable = false, comment = "포인트로 할인된 금액 (미사용 시 0)")
    var pointDiscountPrice: Int = 0,

    // Kotlin 문법상 주 생성자 프로퍼티에는 접근자(private/protected set)를 붙일 수 없다. 컴파일 타임으로
    // 강제하진 않되, 이 필드는 항상 markPaid()/markShipping() 같은 이름 있는 메서드를 통해서만 변경한다
    // (status = X 직접 대입 금지). 단, 외부 API(Toss) 성공 이후 반영되는 동시성 민감한 전이(결제확정/
    // 주문취소/반품완료)는 중복 요청 레이스를 막기 위해 이 엔티티 메서드 대신
    // OrderRepository.updateStatusIfCurrent() 원자적 조건부 UPDATE를 쓴다 (PaymentRecordService,
    // OrderCancelRecordService, OrderReturnRecordService 참고).
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30, comment = "주문 상태")
    var status: OrderStatus = OrderStatus.PENDING_PAYMENT,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0L

    // 주문 생성 시 확정된 쿠폰/포인트 할인액을 반영한다 (OrderService.createOrder() 전용,
    // 재고차감/쿠폰확정/포인트차감이 모두 성공한 뒤 마지막에 한 번만 호출됨)
    fun applyDiscount(couponDiscountPrice: Int, pointDiscountPrice: Int) {
        this.couponDiscountPrice = couponDiscountPrice
        this.pointDiscountPrice = pointDiscountPrice
    }

    fun markPaid() {
        validatePendingPayment()
        status = OrderStatus.PAID
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
            deliveryPrice: Int,
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
