package com.example.springbootkotlinpractice.domain.category.entity

import com.example.springbootkotlinpractice.common.entity.BaseTimeEntity
import com.example.springbootkotlinpractice.enums.CategoryLevel
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "categories", comment = "상품 카테고리 (대/중/소분류 계층 구조)")
class Category(

    @Column(
        name = "parent_id", nullable = true, updatable = false,
        comment = "상위 카테고리 ID (categories.id, 대분류는 NULL)",
    )
    val parentId: Long? = null,

    @Column(name = "name", nullable = false, length = 50, comment = "카테고리 명")
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(
        name = "level", nullable = false, length = 20, updatable = false,
        comment = "카테고리 레벨 (LARGE/MEDIUM/SMALL)",
    )
    val level: CategoryLevel,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0L

    companion object {
        fun of(parentId: Long?, name: String, level: CategoryLevel): Category {
            return Category(
                parentId = parentId,
                name = name,
                level = level,
            )
        }
    }
}
