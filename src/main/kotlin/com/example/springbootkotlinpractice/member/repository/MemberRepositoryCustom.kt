package com.example.springbootkotlinpractice.member.repository

import com.example.springbootkotlinpractice.member.entity.Member

interface MemberRepositoryCustom {

    // 동적 조건 검색 (null 인 조건은 무시)
    fun search(lastName: String?, firstName: String?, email: String?): List<Member>
}
