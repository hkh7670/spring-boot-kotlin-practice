package com.example.springbootkotlinpractice.domain.product.entity

import com.example.springbootkotlinpractice.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "products", comment = "상품 정보")
class Product(

    @Column(name = "name", nullable = false, length = 50, comment = "상품 명")
    var name: String,

    @Column(name = "description", nullable = true, columnDefinition = "TEXT", comment = "상품 상세 설명")
    var description: String? = null,

    @Column(name = "image_url", nullable = true, length = 500, comment = "대표 이미지 URL")
    var imageUrl: String? = null,

    @Column(name = "category_id", nullable = true, comment = "카테고리 ID (categories.id, 소분류)")
    var categoryId: Long? = null,

    @Column(name = "vendor_id", nullable = true, comment = "업체 ID (vendors.id)")
    var vendorId: Long? = null,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", comment = "상품 고유 식별자")
    val id: Long = 0L

    @Column(name = "is_deleted", nullable = false, comment = "삭제 여부 (soft delete, 0: 미삭제, 1: 삭제)")
    var isDeleted: Boolean = false

    fun update(
        name: String,
        description: String?,
        imageUrl: String?,
        categoryId: Long?,
        vendorId: Long?,
    ) {
        this.name = name
        this.description = description
        this.imageUrl = imageUrl
        this.categoryId = categoryId
        this.vendorId = vendorId
    }

    fun delete() {
        isDeleted = true
    }

    companion object {
        fun of(
            name: String,
            description: String? = null,
            imageUrl: String? = null,
            categoryId: Long? = null,
            vendorId: Long? = null,
        ): Product {
            return Product(
                name = name,
                description = description,
                imageUrl = imageUrl,
                categoryId = categoryId,
                vendorId = vendorId,
            )
        }
    }
}
