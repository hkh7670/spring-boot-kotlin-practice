package com.example.springbootkotlinpractice.member.entity

import com.example.springbootkotlinpractice.common.entity.BaseTimeEntity
import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "member")
class Member(

    @Column(name = "uuid", nullable = false, unique = true, updatable = false, length = 36)
    val uuid: String = UUID.randomUUID().toString(),

    @Column(name = "last_name", nullable = false, length = 50)
    var lastName: String,

    @Column(name = "first_name", nullable = false, length = 50)
    var firstName: String,

    @Column(name = "age", nullable = false)
    var age: Int,

    @Column(name = "phone_number", nullable = false, length = 20)
    var phoneNumber: String,

    @Column(name = "email", nullable = false, unique = true, length = 100)
    var email: String,
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
        ): Member {
            return Member(
                lastName = lastName,
                firstName = firstName,
                age = age,
                phoneNumber = phoneNumber,
                email = email,
            )
        }
    }
}
