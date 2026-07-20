package com.example.springbootkotlinpractice.domain.order.entity

import com.example.springbootkotlinpractice.common.entity.BaseTimeEntity
import com.example.springbootkotlinpractice.domain.product.entity.ProductInfo
import jakarta.persistence.*
import org.hibernate.annotations.Comment

@Entity
@Table(name = "order_detail_info")
@Comment("주문 상세 정보")
class OrderDetailInfo(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_info_id")
    @Comment("주문 ID (order_info.id)")
    val orderInfo: OrderInfo,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_info_id")
    @Comment("상품 ID (product_info.id)")
    val productInfo: ProductInfo,

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
            orderInfo: OrderInfo,
            productInfo: ProductInfo,
            price: Long,
            count: Int
        ): OrderDetailInfo {
            return OrderDetailInfo(
                orderInfo = orderInfo,
                productInfo = productInfo,
                price = price,
                count = count,
            )
        }
    }
}
