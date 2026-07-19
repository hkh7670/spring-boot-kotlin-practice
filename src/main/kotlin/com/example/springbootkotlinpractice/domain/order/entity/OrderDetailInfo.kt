package com.example.springbootkotlinpractice.domain.order.entity

import com.example.springbootkotlinpractice.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Comment

@Entity
@Table(name = "order_detail_info")
@Comment("주문 상세 정보")
class OrderDetailInfo(

    @Comment("주문 ID (order_info.id)")
    @Column(name = "order_id", nullable = false, updatable = false)
    val orderId: Long,

    @Comment("상품 ID (product.id)")
    @Column(name = "product_id", nullable = false, updatable = false)
    val productId: Long,

    @Comment("주문 시점의 상품 가격")
    @Column(name = "price", nullable = false, updatable = false)
    val price: Long,

    @Comment("주문 수량")
    @Column(name = "count", nullable = false, updatable = false)
    val count: Int = 1,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0L

    companion object {
        fun of(orderId: Long, productId: Long, price: Long, count: Int): OrderDetailInfo {
            return OrderDetailInfo(
                orderId = orderId,
                productId = productId,
                price = price,
                count = count,
            )
        }
    }
}
