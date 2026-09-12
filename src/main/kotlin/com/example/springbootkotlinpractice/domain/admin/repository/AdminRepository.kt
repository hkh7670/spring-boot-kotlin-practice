package com.example.springbootkotlinpractice.domain.admin.repository

import com.example.springbootkotlinpractice.domain.admin.entity.Admin
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AdminRepository : JpaRepository<Admin, Long> {
    fun existsByEmail(email: String): Boolean

    fun findByEmail(email: String): Admin?
}
