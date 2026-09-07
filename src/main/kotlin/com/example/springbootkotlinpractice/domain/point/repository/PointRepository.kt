package com.example.springbootkotlinpractice.domain.point.repository

import com.example.springbootkotlinpractice.domain.point.entity.Point
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PointRepository : JpaRepository<Point, Long>
