package com.example.springbootkotlinpractice.member.repository

import com.example.springbootkotlinpractice.enums.JoinProvider
import com.example.springbootkotlinpractice.member.entity.Member
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MemberRepository : JpaRepository<Member, Long>, MemberRepositoryCustom {

    fun findByUuid(uuid: String): Member?

    fun findByEmail(email: String): Member?
    fun findByEmailAndJoinProvider(email: String, joinProvider: JoinProvider): Member?
    fun findByProviderIdAndJoinProvider(providerId: String, joinProvider: JoinProvider): Member?
    fun existsByProviderIdAndJoinProvider(
        providerId: String,
        joinProvider: JoinProvider
    ): Boolean

    fun existsByEmail(email: String): Boolean

    fun existsByEmailAndJoinProvider(email: String, joinProvider: JoinProvider): Boolean

    fun existsByUuid(uuid: String): Boolean
}
