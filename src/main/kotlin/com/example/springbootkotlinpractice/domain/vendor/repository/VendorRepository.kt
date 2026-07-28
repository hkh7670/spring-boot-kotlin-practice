package com.example.springbootkotlinpractice.domain.vendor.repository

import com.example.springbootkotlinpractice.domain.vendor.entity.Vendor
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface VendorRepository : JpaRepository<Vendor, Long>
