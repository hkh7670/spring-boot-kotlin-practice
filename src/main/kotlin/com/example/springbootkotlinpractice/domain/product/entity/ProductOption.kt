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
import org.hibernate.annotations.DynamicUpdate

// 재고는 주문 시 원자적 UPDATE로 차감되므로, 관리자가 가격만 수정해도 오래된 stock_count가
// 덮어써지지 않도록 변경된 컬럼만 UPDATE한다
@DynamicUpdate
@Entity
@Table(name = "product_options", comment = "상품 옵션(변형) 정보")
class ProductOption(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", comment = "상품 ID (products.id)")
    val product: Product,

    @Column(name = "name", nullable = false, length = 100, comment = "옵션 명 (예: 블랙 / L사이즈)")
    var name: String,

    @Column(name = "price", nullable = false, comment = "옵션별 가격")
    var price: Int,

    @Column(name = "stock_count", nullable = false, comment = "옵션별 재고 수량")
    var stockCount: Int = 0,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", comment = "상품 옵션 고유 식별자")
    val id: Long = 0L

    // stockCount가 null이면 재고를 건드리지 않는다(@DynamicUpdate와 함께 동시 주문의 차감분 보호)
    fun update(name: String, price: Int, stockCount: Int?) {
        this.name = name
        this.price = price
        stockCount?.let { this.stockCount = it }
    }

    companion object {
        fun of(product: Product, name: String, price: Int, stockCount: Int = 0): ProductOption {
            return ProductOption(
                product = product,
                name = name,
                price = price,
                stockCount = stockCount,
            )
        }
    }
}
