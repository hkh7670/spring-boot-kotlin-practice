package com.example.springbootkotlinpractice.domain.member.repository

import com.example.springbootkotlinpractice.enums.JoinProvider
import com.example.springbootkotlinpractice.domain.member.entity.Member
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

    // 암호화된 PII를 로딩하지 않고 존재하는 회원 수만 센다(관리자 대량 발급 대상 검증용)
    fun countByIdIn(ids: Collection<Long>): Long
}
