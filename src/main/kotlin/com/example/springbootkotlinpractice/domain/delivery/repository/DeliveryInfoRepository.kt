package com.example.springbootkotlinpractice.domain.delivery.repository

import com.example.springbootkotlinpractice.domain.delivery.entity.DeliveryInfo
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DeliveryInfoRepository : JpaRepository<DeliveryInfo, Long>
