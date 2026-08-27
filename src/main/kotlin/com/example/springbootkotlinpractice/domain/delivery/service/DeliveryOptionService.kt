package com.example.springbootkotlinpractice.domain.delivery.service

import com.example.springbootkotlinpractice.domain.delivery.dto.DeliveryOptionResponse
import com.example.springbootkotlinpractice.domain.delivery.repository.DeliveryOptionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeliveryOptionService(
    private val deliveryOptionRepository: DeliveryOptionRepository,
) {

    @Transactional(readOnly = true)
    fun getDeliveryOptions(): List<DeliveryOptionResponse> {
        return deliveryOptionRepository.findAll().map {
            DeliveryOptionResponse(
                id = it.id,
                name = it.name,
                price = it.price,
            )
        }
    }
}
