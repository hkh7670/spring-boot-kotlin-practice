package com.example.springbootkotlinpractice.domain.point.entity

import com.example.springbootkotlinpractice.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "points", comment = "포인트 템플릿(지급 종류) 정보")
class Point(

    @Column(name = "name", nullable = false, length = 100, comment = "포인트 지급 사유/종류 명")
    var name: String,

    @Column(name = "valid_days", nullable = true, comment = "발급일로부터 유효 일수 (NULL이면 무제한)")
    var validDays: Int? = null,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0L

    companion object {
        fun of(name: String, validDays: Int? = null): Point {
            return Point(
                name = name,
                validDays = validDays,
            )
        }
    }
}
