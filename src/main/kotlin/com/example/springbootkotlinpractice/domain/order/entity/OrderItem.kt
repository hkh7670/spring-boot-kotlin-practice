package com.example.springbootkotlinpractice.domain.order.entity

import com.example.springbootkotlinpractice.common.entity.BaseTimeEntity
import com.example.springbootkotlinpractice.domain.product.entity.Product
import jakarta.persistence.*
import org.hibernate.annotations.Comment

@Entity
@Table(name = "order_items")
@Comment("주문 상품 정보")
class OrderItem(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    @Comment("주문 ID (orders.id)")
    val order: Order,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    @Comment("상품 ID (products.id)")
    val product: Product,

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
        fun of(
            order: Order,
            product: Product,
            price: Long,
            count: Int
        ): OrderItem {
            return OrderItem(
                order = order,
                product = product,
                price = price,
                count = count,
            )
        }
    }
}
