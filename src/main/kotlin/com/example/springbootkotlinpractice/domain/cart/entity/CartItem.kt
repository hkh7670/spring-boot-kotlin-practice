package com.example.springbootkotlinpractice.domain.cart.entity

import com.example.springbootkotlinpractice.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.Comment

@Entity
@Table(
    name = "cart_items",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_cart_items_01", columnNames = ["member_id", "product_id"]),
    ],
)
@Comment("회원별 장바구니 상품")
class CartItem(

    @Comment("장바구니 소유 회원 ID (members.id)")
    @Column(name = "member_id", nullable = false, updatable = false)
    val memberId: Long,

    @Comment("상품 ID (products.id)")
    @Column(name = "product_id", nullable = false, updatable = false)
    val productId: Long,

    @Comment("담은 수량")
    @Column(name = "count", nullable = false)
    var count: Int,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0L

    companion object {
        fun of(memberId: Long, productId: Long, count: Int): CartItem {
            return CartItem(
                memberId = memberId,
                productId = productId,
                count = count,
            )
        }
    }
}
