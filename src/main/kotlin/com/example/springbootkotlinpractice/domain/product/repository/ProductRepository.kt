package com.example.springbootkotlinpractice.domain.product.repository

import com.example.springbootkotlinpractice.domain.product.entity.Product
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProductRepository : JpaRepository<Product, Long>, ProductRepositoryCustom
