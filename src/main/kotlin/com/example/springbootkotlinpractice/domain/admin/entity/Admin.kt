package com.example.springbootkotlinpractice.domain.admin.entity

import com.example.springbootkotlinpractice.common.converter.Aes256Converter
import com.example.springbootkotlinpractice.common.entity.BaseTimeEntity
import com.example.springbootkotlinpractice.enums.AdminStatus
import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.exception.ApiErrorException
import jakarta.persistence.*
import java.time.LocalDateTime
import org.hibernate.annotations.Comment

@Entity
@Table(
    name = "admins",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_admins_01", columnNames = ["email"]),
    ]
)
@Comment("관리자 정보")
class Admin(

    @Comment("이름 (AES 암호화 저장)")
    @Convert(converter = Aes256Converter::class)
    @Column(name = "name", nullable = false, length = 100)
    var name: String,

    @Comment("이메일 (AES 암호화 저장)")
    @Convert(converter = Aes256Converter::class)
    @Column(name = "email", nullable = false, length = 100)
    val email: String,

    @Comment("비밀번호 (BCrypt 해시)")
    @Column(name = "password", nullable = false, length = 100)
    var password: String,

    @Comment("계정 상태")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: AdminStatus = AdminStatus.ACTIVE,

    @Comment("탈퇴 일시 (개인정보 파기 배치 기준일)")
    @Column(name = "withdrawn_datetime", nullable = true)
    var withdrawnDatetime: LocalDateTime? = null,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Comment("관리자 고유 식별자")
    val id: Long = 0L

    fun markInactive() {
        when (status) {
            AdminStatus.ACTIVE -> status = AdminStatus.INACTIVE
            AdminStatus.INACTIVE -> throw ApiErrorException(ResponseCodeEnum.ADMIN_ALREADY_INACTIVE)
            AdminStatus.WITHDRAWN -> throw ApiErrorException(ResponseCodeEnum.ADMIN_ALREADY_WITHDRAWN)
        }
    }

    fun reactivate() {
        when (status) {
            AdminStatus.INACTIVE -> status = AdminStatus.ACTIVE
            AdminStatus.ACTIVE -> throw ApiErrorException(ResponseCodeEnum.ADMIN_NOT_INACTIVE)
            AdminStatus.WITHDRAWN -> throw ApiErrorException(ResponseCodeEnum.ADMIN_ALREADY_WITHDRAWN)
        }
    }

    fun withdraw() {
        when (status) {
            AdminStatus.ACTIVE, AdminStatus.INACTIVE -> {
                status = AdminStatus.WITHDRAWN
                withdrawnDatetime = LocalDateTime.now()
            }
            AdminStatus.WITHDRAWN -> throw ApiErrorException(ResponseCodeEnum.ADMIN_ALREADY_WITHDRAWN)
        }
    }

    companion object {
        fun of(
            name: String,
            email: String,
            password: String,
        ): Admin {
            return Admin(
                name = name,
                email = email,
                password = password,
            )
        }
    }
}
