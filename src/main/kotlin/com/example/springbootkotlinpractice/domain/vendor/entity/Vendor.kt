package com.example.springbootkotlinpractice.domain.vendor.entity

import com.example.springbootkotlinpractice.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "vendors", comment = "상품 업체(공급사) 정보")
class Vendor(

    @Column(name = "name", nullable = false, length = 100, comment = "업체명")
    var name: String,

    @Column(
        name = "business_registration_number", nullable = true, length = 20,
        comment = "사업자등록번호",
    )
    var businessRegistrationNumber: String? = null,

    @Column(name = "contact_number", nullable = true, length = 20, comment = "업체 연락처")
    var contactNumber: String? = null,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0L

    companion object {
        fun of(
            name: String,
            businessRegistrationNumber: String? = null,
            contactNumber: String? = null,
        ): Vendor {
            return Vendor(
                name = name,
                businessRegistrationNumber = businessRegistrationNumber,
                contactNumber = contactNumber,
            )
        }
    }
}
