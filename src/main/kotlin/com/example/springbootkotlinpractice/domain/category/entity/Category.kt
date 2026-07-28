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
import org.hibernate.annotations.Comment

@Entity
@Table(name = "categories")
@Comment("상품 카테고리 (대/중/소분류 계층 구조)")
class Category(

    @Comment("상위 카테고리 ID (categories.id, 대분류는 NULL)")
    @Column(name = "parent_id", nullable = true, updatable = false)
    val parentId: Long? = null,

    @Comment("카테고리 명")
    @Column(name = "name", nullable = false, length = 50)
    var name: String,

    @Comment("카테고리 레벨 (LARGE/MEDIUM/SMALL)")
    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false, length = 20, updatable = false)
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
