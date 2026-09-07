package com.example.springbootkotlinpractice.domain.member.entity

import com.example.springbootkotlinpractice.common.converter.Aes256Converter
import com.example.springbootkotlinpractice.common.entity.BaseTimeEntity
import com.example.springbootkotlinpractice.enums.JoinProvider
import com.example.springbootkotlinpractice.enums.Role
import jakarta.persistence.*
import java.time.LocalDate
import java.util.*

@Entity
@Table(
    name = "members",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_members_01", columnNames = ["uuid"]),
        UniqueConstraint(name = "uq_members_02", columnNames = ["provider_id", "join_provider"]),
        UniqueConstraint(name = "uq_members_03", columnNames = ["email", "join_provider"]),
    ],
    comment = "회원 정보",
)
class Member(

    @Column(name = "uuid", nullable = false, updatable = false, length = 36, comment = "외부 노출용 UUID")
    val uuid: String = UUID.randomUUID().toString(),

    @Column(
        name = "provider_id", nullable = true, updatable = false, length = 100,
        comment = "OAuth 제공자에서 발급한 사용자 ID",
    )
    val providerId: String? = null,

    @Convert(converter = Aes256Converter::class)
    @Column(name = "last_name", nullable = false, length = 100, comment = "성 (AES 암호화 저장)")
    var lastName: String,

    @Convert(converter = Aes256Converter::class)
    @Column(name = "first_name", nullable = false, length = 100, comment = "이름 (AES 암호화 저장)")
    var firstName: String,

    @Column(name = "birth_date", nullable = false, comment = "생년월일")
    var birthDate: LocalDate,

    @Convert(converter = Aes256Converter::class)
    @Column(name = "phone_number", nullable = false, length = 100, comment = "전화번호 (AES 암호화 저장)")
    var phoneNumber: String,

    @Convert(converter = Aes256Converter::class)
    @Column(name = "email", nullable = true, length = 100, comment = "이메일 (AES 암호화 저장)")
    var email: String? = null,

    @Column(
        name = "password", nullable = true, length = 100,
        comment = "비밀번호 (BCrypt 해시, EMAIL 가입 회원만 보유)",
    )
    var password: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(
        name = "join_provider", nullable = false, length = 20,
        comment = "가입 경로 (일반/OAuth 제공자 구분)",
    )
    val joinProvider: JoinProvider,

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20, comment = "회원 권한 (USER, ADMIN 등)")
    val role: Role = Role.USER,

    @Convert(converter = Aes256Converter::class)
    @Column(
        name = "totp_secret", nullable = true, length = 255,
        comment = "TOTP 시크릿 키 (AES 암호화 저장, 활성화 시에만 존재)",
    )
    var totpSecret: String? = null,

    @Column(name = "totp_enabled", nullable = false, comment = "TOTP 2단계 인증 활성화 여부")
    var totpEnabled: Boolean = false,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", comment = "회원 고유 식별자")
    val id: Long = 0L

    // TOTP 등록 확정 시 호출 — 시크릿 저장과 활성화를 한 번에 처리한다
    fun enableTotp(secret: String) {
        this.totpSecret = secret
        this.totpEnabled = true
    }

    // TOTP 비활성화 시 호출 — 시크릿을 완전히 제거한다
    fun disableTotp() {
        this.totpSecret = null
        this.totpEnabled = false
    }

    companion object {
        fun of(
            lastName: String,
            firstName: String,
            birthDate: LocalDate,
            phoneNumber: String,
            email: String,
            password: String,
            joinProvider: JoinProvider,
        ): Member {
            return Member(
                lastName = lastName,
                firstName = firstName,
                birthDate = birthDate,
                phoneNumber = phoneNumber,
                email = email,
                password = password,
                joinProvider = joinProvider,
            )
        }

        fun ofOAuth(
            providerId: String,
            email: String?,
            lastName: String,
            firstName: String,
            birthDate: LocalDate,
            phoneNumber: String,
            joinProvider: JoinProvider,
        ): Member {
            return Member(
                providerId = providerId,
                lastName = lastName,
                firstName = firstName,
                birthDate = birthDate,
                phoneNumber = phoneNumber,
                email = email,
                joinProvider = joinProvider,
            )
        }
    }
}
