package com.example.springbootkotlinpractice.member.entity

import com.example.springbootkotlinpractice.common.converter.Aes256Converter
import com.example.springbootkotlinpractice.common.entity.BaseTimeEntity
import com.example.springbootkotlinpractice.enums.JoinProvider
import com.example.springbootkotlinpractice.enums.Role
import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "member")
class Member(

    @Column(name = "uuid", nullable = false, unique = true, updatable = false, length = 36)
    val uuid: String = UUID.randomUUID().toString(),

    @Convert(converter = Aes256Converter::class)
    @Column(name = "last_name", nullable = false, length = 100)
    var lastName: String,

    @Convert(converter = Aes256Converter::class)
    @Column(name = "first_name", nullable = false, length = 100)
    var firstName: String,

    @Column(name = "age", nullable = false)
    var age: Int,

    @Convert(converter = Aes256Converter::class)
    @Column(name = "phone_number", nullable = false, length = 100)
    var phoneNumber: String,

    @Convert(converter = Aes256Converter::class)
    @Column(name = "email", nullable = false, unique = true, length = 100)
    var email: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "join_provider", nullable = false, length = 20)
    val joinProvider: JoinProvider,

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    val role: Role = Role.USER,
) : BaseTimeEntity() {

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

        // OAuth 최초 로그인 시 provider 가 제공하지 않는 나이/전화번호는 빈 값으로 채워 가입시킨다
        fun ofOAuth(
            email: String,
            nickname: String,
            joinProvider: JoinProvider,
        ): Member {
            return Member(
                lastName = "",
                firstName = nickname,
                age = 0,
                phoneNumber = "",
                email = email,
                joinProvider = joinProvider,
            )
        }
    }
}
