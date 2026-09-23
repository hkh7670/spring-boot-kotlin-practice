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

    @Column(name = "is_deleted", nullable = false, comment = "삭제 여부 (soft delete, 0: 미삭제, 1: 삭제)")
    var isDeleted: Boolean = false

    // 유효 일수는 적립 시점에 만료 일시로 계산되어 member_points에 고정되므로, 수정해도 기존 적립분은 바뀌지 않는다
    fun update(name: String, validDays: Int?) {
        this.name = name
        this.validDays = validDays
    }

    fun delete() {
        isDeleted = true
    }

    companion object {
        fun of(name: String, validDays: Int? = null): Point {
            return Point(
                name = name,
                validDays = validDays,
            )
        }
    }
}
