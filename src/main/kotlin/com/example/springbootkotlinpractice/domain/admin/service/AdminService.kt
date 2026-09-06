package com.example.springbootkotlinpractice.domain.admin.service

import com.example.springbootkotlinpractice.domain.admin.dto.AdminCreateRequest
import com.example.springbootkotlinpractice.domain.admin.dto.AdminCreateResponse
import com.example.springbootkotlinpractice.domain.admin.entity.Admin
import com.example.springbootkotlinpractice.domain.admin.repository.AdminRepository
import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.exception.ApiErrorException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminService(
    private val adminRepository: AdminRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    @Transactional
    fun createAdmin(request: AdminCreateRequest): AdminCreateResponse {
        if (adminRepository.existsByEmail(request.email)) {
            throw ApiErrorException(ResponseCodeEnum.DUPLICATED_EMAIL)
        }
        val admin = try {
            adminRepository.save(
                Admin.of(
                    name = request.name,
                    email = request.email,
                    password = passwordEncoder.encode(request.password)
                        ?: throw ApiErrorException(ResponseCodeEnum.INTERNAL_SERVER_ERROR),
                )
            )
        } catch (e: DataIntegrityViolationException) {
            throw ApiErrorException(ResponseCodeEnum.DUPLICATED_EMAIL)
        }
        return AdminCreateResponse.from(admin)
    }
}
