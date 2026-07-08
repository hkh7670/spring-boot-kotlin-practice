package com.example.springbootkotlinpractice.common.entity

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import java.time.LocalDateTime
import org.hibernate.annotations.Comment
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseTimeEntity {

    @Comment("생성 일시")
    @CreatedDate
    @Column(name = "created_datetime", nullable = false, updatable = false)
    lateinit var createdDatetime: LocalDateTime
        protected set

    @Comment("수정 일시")
    @LastModifiedDate
    @Column(name = "updated_datetime", nullable = false)
    lateinit var updatedDatetime: LocalDateTime
        protected set
}
