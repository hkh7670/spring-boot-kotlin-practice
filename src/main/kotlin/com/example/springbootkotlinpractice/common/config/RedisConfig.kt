package com.example.springbootkotlinpractice.common.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory

@Configuration
@EnableConfigurationProperties(RedisProperties::class)
class RedisConfig(
    private val redisProperties: RedisProperties,
) {
    // StringRedisTemplate 은 이 RedisConnectionFactory 를 기반으로 Spring Boot 가 자동 구성한다
    @Bean
    fun redisConnectionFactory(): RedisConnectionFactory {
        val configuration = RedisStandaloneConfiguration(redisProperties.host, redisProperties.port).apply {
            setPassword(redisProperties.password)
        }
        return LettuceConnectionFactory(configuration)
    }
}
