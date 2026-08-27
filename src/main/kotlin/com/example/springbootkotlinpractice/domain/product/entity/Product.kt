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

    @Comment("상품 상세 설명")
    @Column(name = "description", nullable = true, columnDefinition = "TEXT")
    var description: String? = null,

    @Comment("대표 이미지 URL")
    @Column(name = "image_url", nullable = true, length = 500)
    var imageUrl: String? = null,

    @Comment("카테고리 ID (categories.id, 소분류)")
    @Column(name = "category_id", nullable = true)
    var categoryId: Long? = null,

    @Comment("업체 ID (vendors.id)")
    @Column(name = "vendor_id", nullable = true)
    var vendorId: Long? = null,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Comment("상품 고유 식별자")
    val id: Long = 0L

    companion object {
        fun of(
            name: String,
            price: Int,
            stockCount: Int = 0,
            description: String? = null,
            imageUrl: String? = null,
            categoryId: Long? = null,
            vendorId: Long? = null,
        ): Product {
            return Product(
                name = name,
                price = price,
                stockCount = stockCount,
                description = description,
                imageUrl = imageUrl,
                categoryId = categoryId,
                vendorId = vendorId,
            )
        }
    }
}
