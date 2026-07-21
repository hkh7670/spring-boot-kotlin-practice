package com.example.springbootkotlinpractice.domain.order.entity

import com.example.springbootkotlinpractice.common.entity.BaseTimeEntity
import com.example.springbootkotlinpractice.enums.OrderStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Comment

@Entity
@Table(name = "order_status_histories")
@Comment("주문 상태 변경 이력")
class OrderStatusHistory(

    @Comment("주문 ID (orders.id)")
    @Column(name = "order_id", nullable = false, updatable = false)
    val orderId: Long,

    @Comment("변경된 주문 상태")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30, updatable = false)
    val status: OrderStatus,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0L

    companion object {
        fun of(orderId: Long, status: OrderStatus): OrderStatusHistory {
            return OrderStatusHistory(
                orderId = orderId,
                status = status,
            )
        }
    }
}
