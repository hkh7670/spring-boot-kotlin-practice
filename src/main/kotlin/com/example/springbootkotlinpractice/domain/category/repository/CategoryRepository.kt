package com.example.springbootkotlinpractice.domain.category.repository

import com.example.springbootkotlinpractice.domain.category.entity.Category
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CategoryRepository : JpaRepository<Category, Long>
