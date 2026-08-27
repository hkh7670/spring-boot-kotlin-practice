package com.example.springbootkotlinpractice.domain.product.repository

import com.example.springbootkotlinpractice.domain.product.entity.ProductOption
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ProductOptionRepository : JpaRepository<ProductOption, Long> {

    fun findByProductId(productId: Long): List<ProductOption>

    // 상품 목록 화면의 상품별 재고 합계(품절 배지) + 최저가("N원부터")용 배치 집계
    @Query(
        "SELECT po.product.id AS productId, COALESCE(SUM(po.stockCount), 0) AS totalStock, " +
                "COALESCE(MIN(po.price), 0) AS minPrice " +
                "FROM ProductOption po WHERE po.product.id IN :productIds GROUP BY po.product.id"
    )
    fun findAggregatesByProductIdIn(@Param("productIds") productIds: List<Long>): List<ProductOptionAggregate>

    // 단건 조회 시 Product를 함께 fetch join (주문 생성 시 가격 계산에 필요)
    @Query("SELECT po FROM ProductOption po JOIN FETCH po.product WHERE po.id = :id")
    fun findByIdFetchProduct(@Param("id") id: Long): ProductOption?

    // 장바구니 조회용 배치 fetch join (N+1 회피)
    @Query("SELECT po FROM ProductOption po JOIN FETCH po.product WHERE po.id IN :ids")
    fun findByIdInFetchProduct(@Param("ids") ids: List<Long>): List<ProductOption>

    // 재고가 충분할 때만 원자적으로 차감한다 (동시 주문에 의한 초과 판매 방지). 반환값이 0이면 재고 부족을 의미한다.
    // 벌크 UPDATE는 JPA Auditing(@LastModifiedDate)을 안 타므로 updated_datetime을 직접 갱신한다.
    @Modifying
    @Query(
        value = "UPDATE product_options SET stock_count = stock_count - :count, updated_datetime = NOW(6) " +
                "WHERE id = :productOptionId AND stock_count >= :count",
        nativeQuery = true,
    )
    fun decreaseStock(
        @Param("productOptionId") productOptionId: Long,
        @Param("count") count: Int
    ): Int

    // 결제 실패/취소/반품으로 차감된 재고를 복구한다.
    @Modifying
    @Query(
        value = "UPDATE product_options SET stock_count = stock_count + :count, updated_datetime = NOW(6) " +
                "WHERE id = :productOptionId",
        nativeQuery = true,
    )
    fun increaseStock(
        @Param("productOptionId") productOptionId: Long,
        @Param("count") count: Int
    ): Int
}

interface ProductOptionAggregate {
    fun getProductId(): Long
    fun getTotalStock(): Long
    fun getMinPrice(): Long
}
