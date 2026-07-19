package com.example.springbootkotlinpractice.domain.member.repository

import com.example.springbootkotlinpractice.domain.member.entity.Member
import com.example.springbootkotlinpractice.domain.member.entity.QMember.member
import com.querydsl.jpa.impl.JPAQueryFactory

class MemberRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : MemberRepositoryCustom {

    override fun search(lastName: String?, firstName: String?, email: String?): List<Member> {
        return queryFactory
            .selectFrom(member)
            .where(
                lastName?.let { member.lastName.eq(it) },
                firstName?.let { member.firstName.eq(it) },
                email?.let { member.email.eq(it) },
            )
            .orderBy(member.id.desc())
            .fetch()
    }
}
