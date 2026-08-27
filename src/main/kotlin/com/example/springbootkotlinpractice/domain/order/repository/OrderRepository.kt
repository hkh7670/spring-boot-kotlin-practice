package com.example.springbootkotlinpractice.domain.order.repository

import com.example.springbootkotlinpractice.domain.order.entity.Order
import com.example.springbootkotlinpractice.enums.OrderStatus
import java.time.LocalDateTime
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface OrderRepository : JpaRepository<Order, Long> {

    fun findByOrderUid(orderUid: String): Order?

    fun findByIdAndMemberId(id: Long, memberId: Long): Order?

    fun findByStatusAndCreatedDatetimeBefore(status: OrderStatus, createdDatetime: LocalDateTime): List<Order>

    fun findByMemberId(memberId: Long, pageable: Pageable): Page<Order>

    // 현재 상태가 expectedStatus일 때만 newStatus로 원자적으로 전이하고 영향받은 row 수를 반환한다
    // (decreaseStock()과 동일한 조건부 UPDATE 패턴). 0이면 이미 다른 동시 요청이 처리했다는 뜻이므로
    // 호출자는 재고복구/이벤트발행 같은 후속 부수효과를 건너뛰어야 한다 — 중복 요청(더블클릭, 네트워크
    // 재시도, React StrictMode 이중 마운트 등)에 대한 동시성 방어용.
    @Modifying
    @Query("UPDATE Order o SET o.status = :newStatus WHERE o.id = :orderId AND o.status = :expectedStatus")
    fun updateStatusIfCurrent(
        @Param("orderId") orderId: Long,
        @Param("expectedStatus") expectedStatus: OrderStatus,
        @Param("newStatus") newStatus: OrderStatus,
    ): Int
}
