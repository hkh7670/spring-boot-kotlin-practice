package com.example.springbootkotlinpractice.domain.cart.entity

import com.example.springbootkotlinpractice.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "cart_items",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_cart_items_01", columnNames = ["member_id", "product_option_id"]),
    ],
    comment = "회원별 장바구니 상품",
)
class CartItem(

    @Column(
        name = "member_id", nullable = false, updatable = false,
        comment = "장바구니 소유 회원 ID (members.id)",
    )
    val memberId: Long,

    @Column(
        name = "product_option_id", nullable = false, updatable = false,
        comment = "상품 옵션 ID (product_options.id)",
    )
    val productOptionId: Long,

    @Column(name = "count", nullable = false, comment = "담은 수량")
    var count: Int,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0L

    companion object {
        fun of(memberId: Long, productOptionId: Long, count: Int): CartItem {
            return CartItem(
                memberId = memberId,
                productOptionId = productOptionId,
                count = count,
            )
        }
    }
}
