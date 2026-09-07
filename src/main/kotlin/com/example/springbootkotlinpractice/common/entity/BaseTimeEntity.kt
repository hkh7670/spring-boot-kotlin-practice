package com.example.springbootkotlinpractice.common.entity

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseTimeEntity {

    @CreatedDate
    @Column(name = "created_datetime", nullable = false, updatable = false, comment = "생성 일시")
    lateinit var createdDatetime: LocalDateTime
        protected set

    @LastModifiedDate
    @Column(name = "updated_datetime", nullable = false, comment = "수정 일시")
    lateinit var updatedDatetime: LocalDateTime
        protected set
}
