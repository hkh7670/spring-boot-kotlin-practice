package com.example.springbootkotlinpractice.domain.delivery.entity

import com.example.springbootkotlinpractice.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "delivery_options", comment = "배송 옵션 관련 정보")
class DeliveryOption(

    @Column(name = "name", nullable = false, length = 50, comment = "배송 옵션 명")
    var name: String,

    @Column(name = "price", nullable = false, comment = "배송 가격")
    var price: Int,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", comment = "배송 정보 고유 식별자")
    val id: Long = 0L

    companion object {
        fun of(name: String, price: Int): DeliveryOption {
            return DeliveryOption(name = name, price = price)
        }
    }
}
