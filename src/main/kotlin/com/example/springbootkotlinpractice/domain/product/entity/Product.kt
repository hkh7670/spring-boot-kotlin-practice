package com.example.springbootkotlinpractice.domain.product.entity

import com.example.springbootkotlinpractice.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Comment

@Entity
@Table(name = "products")
@Comment("상품 정보")
class Product(

    @Comment("상품 명")
    @Column(name = "name", nullable = false, length = 50)
    var name: String,

    @Comment("가격")
    @Column(name = "price", nullable = false)
    var price: Int,

    @Comment("재고 수량")
    @Column(name = "stock_count", nullable = false)
    var stockCount: Int = 0,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Comment("상품 고유 식별자")
    val id: Long = 0L
}
