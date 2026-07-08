package com.example.springbootkotlinpractice.member.entity

import com.example.springbootkotlinpractice.common.converter.Aes256Converter
import com.example.springbootkotlinpractice.common.entity.BaseTimeEntity
import com.example.springbootkotlinpractice.enums.JoinProvider
import com.example.springbootkotlinpractice.enums.Role
import jakarta.persistence.*
import java.util.*
import org.hibernate.annotations.Comment

@Entity
@Table(
    name = "member",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_member_provider_id_join_provider", columnNames = ["provider_id", "join_provider"])
    ]
)
@Comment("회원 정보")
class Member(

    @Comment("외부 노출용 UUID")
    @Column(name = "uuid", nullable = false, unique = true, updatable = false, length = 36)
    val uuid: String = UUID.randomUUID().toString(),

    @Comment("OAuth 제공자에서 발급한 사용자 ID")
    @Column(name = "provider_id", nullable = true, updatable = false, length = 100)
    val providerId: String? = null,

    @Comment("성 (AES 암호화 저장)")
    @Convert(converter = Aes256Converter::class)
    @Column(name = "last_name", nullable = false, length = 100)
    var lastName: String,

    @Comment("이름 (AES 암호화 저장)")
    @Convert(converter = Aes256Converter::class)
    @Column(name = "first_name", nullable = false, length = 100)
    var firstName: String,

    @Comment("나이")
    @Column(name = "age", nullable = false)
    var age: Int,

    @Comment("전화번호 (AES 암호화 저장)")
    @Convert(converter = Aes256Converter::class)
    @Column(name = "phone_number", nullable = false, length = 100)
    var phoneNumber: String,

    @Comment("이메일 (AES 암호화 저장)")
    @Convert(converter = Aes256Converter::class)
    @Column(name = "email", nullable = true, length = 100)
    var email: String? = null,

    @Comment("가입 경로 (일반/OAuth 제공자 구분)")
    @Enumerated(EnumType.STRING)
    @Column(name = "join_provider", nullable = false, length = 20)
    val joinProvider: JoinProvider,

    @Comment("회원 권한 (USER, ADMIN 등)")
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    val role: Role = Role.USER,
) : BaseTimeEntity() {

    @Comment("회원 고유 식별자")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0L

    companion object {
        fun of(
            lastName: String,
            firstName: String,
            age: Int,
            phoneNumber: String,
            email: String,
            joinProvider: JoinProvider,
        ): Member {
            return Member(
                lastName = lastName,
                firstName = firstName,
                age = age,
                phoneNumber = phoneNumber,
                email = email,
                joinProvider = joinProvider,
            )
        }

        fun ofOAuth(
            providerId: String,
            email: String?,
            lastName: String,
            firstName: String,
            age: Int,
            phoneNumber: String,
            joinProvider: JoinProvider,
        ): Member {
            return Member(
                providerId = providerId,
                lastName = lastName,
                firstName = firstName,
                age = age,
                phoneNumber = phoneNumber,
                email = email,
                joinProvider = joinProvider,
            )
        }
    }
}
