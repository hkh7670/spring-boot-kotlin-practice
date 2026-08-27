package com.example.springbootkotlinpractice.domain.product.entity

import com.example.springbootkotlinpractice.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Comment

@Entity
@Table(name = "product_options")
@Comment("상품 옵션(변형) 정보")
class ProductOption(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    @Comment("상품 ID (products.id)")
    val product: Product,

    @Comment("옵션 명 (예: 블랙 / L사이즈)")
    @Column(name = "name", nullable = false, length = 100)
    val name: String,

    @Comment("옵션별 재고 수량")
    @Column(name = "stock_count", nullable = false)
    var stockCount: Int = 0,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Comment("상품 옵션 고유 식별자")
    val id: Long = 0L

    companion object {
        fun of(product: Product, name: String, stockCount: Int = 0): ProductOption {
            return ProductOption(
                product = product,
                name = name,
                stockCount = stockCount,
            )
        }
    }
}
