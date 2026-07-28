package com.example.springbootkotlinpractice.domain.vendor.entity

import com.example.springbootkotlinpractice.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Comment

@Entity
@Table(name = "vendors")
@Comment("상품 업체(공급사) 정보")
class Vendor(

    @Comment("업체명")
    @Column(name = "name", nullable = false, length = 100)
    var name: String,

    @Comment("사업자등록번호")
    @Column(name = "business_registration_number", nullable = true, length = 20)
    var businessRegistrationNumber: String? = null,

    @Comment("업체 연락처")
    @Column(name = "contact_number", nullable = true, length = 20)
    var contactNumber: String? = null,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0L
}
