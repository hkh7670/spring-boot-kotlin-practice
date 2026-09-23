package com.example.springbootkotlinpractice.domain.point.repository

import com.example.springbootkotlinpractice.domain.point.entity.Point
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PointRepository : JpaRepository<Point, Long> {

    fun findByIdAndIsDeletedFalse(id: Long): Point?

    fun findAllByIsDeletedFalse(pageable: Pageable): Page<Point>
}
