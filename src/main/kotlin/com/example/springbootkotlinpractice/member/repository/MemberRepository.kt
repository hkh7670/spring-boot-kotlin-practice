package com.example.springbootkotlinpractice.member.repository

import com.example.springbootkotlinpractice.member.entity.Member
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MemberRepository : JpaRepository<Member, Long> {

    fun findByUuid(uuid: String): Member?

    fun findByEmail(email: String): Member?

    fun existsByEmail(email: String): Boolean

    fun existsByUuid(uuid: String): Boolean
}
