package com.example.springbootkotlinpractice.common

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthCheckController {

    @GetMapping("/server-health-check")
    fun serverHealthCheck(): ResponseEntity<Unit> {
        return ResponseEntity.ok().build()
    }
}