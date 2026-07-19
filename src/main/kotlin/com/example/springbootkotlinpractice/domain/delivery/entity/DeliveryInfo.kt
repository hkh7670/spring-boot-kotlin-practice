package com.example.springbootkotlinpractice.domain.delivery.entity

import com.example.springbootkotlinpractice.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Comment

@Entity
@Table(name = "delivery_info")
@Comment("배송 관련 정보")
class DeliveryInfo(

    @Comment("배송 옵션 명")
    @Column(name = "name", nullable = false, length = 50)
    var name: String,

    @Comment("배송 가격")
    @Column(name = "price", nullable = false)
    var price: Int,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Comment("배송 정보 고유 식별자")
    val id: Long = 0L
}
