package com.example.springbootkotlinpractice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@EnableJpaAuditing
@SpringBootApplication
class SpringBootKotlinPracticeApplication

fun main(args: Array<String>) {
    runApplication<SpringBootKotlinPracticeApplication>(*args)
}
