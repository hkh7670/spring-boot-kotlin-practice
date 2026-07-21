package com.example.springbootkotlinpractice.domain.product.repository

import com.example.springbootkotlinpractice.domain.product.entity.Product
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ProductRepository : JpaRepository<Product, Long> {

    // 재고가 충분할 때만 원자적으로 차감한다 (동시 주문에 의한 초과 판매 방지). 반환값이 0이면 재고 부족을 의미한다.
    @Modifying
    @Query(
        "UPDATE Product p SET p.stockCount = p.stockCount - :count " +
                "WHERE p.id = :productId AND p.stockCount >= :count"
    )
    fun decreaseStock(
        @Param("productId") productId: Long,
        @Param("count") count: Int
    ): Int

    // 결제 실패로 주문이 취소될 때 차감된 재고를 복구한다.
    @Modifying
    @Query("UPDATE Product p SET p.stockCount = p.stockCount + :count WHERE p.id = :productId")
    fun increaseStock(
        @Param("productId") productId: Long,
        @Param("count") count: Int
    ): Int
}
