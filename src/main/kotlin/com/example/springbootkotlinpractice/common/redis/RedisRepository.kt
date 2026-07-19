package com.example.springbootkotlinpractice.common.redis

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
class RedisRepository(
    private val redisTemplate: StringRedisTemplate,
) {

    fun save(key: String, value: String) {
        redisTemplate.opsForValue().set(key, value)
    }

    fun save(key: String, value: String, ttl: Duration) {
        redisTemplate.opsForValue().set(key, value, ttl)
    }

    fun find(key: String): String? {
        return redisTemplate.opsForValue().get(key)
    }

    fun exists(key: String): Boolean {
        return redisTemplate.hasKey(key) == true
    }

    fun delete(key: String): Boolean {
        return redisTemplate.delete(key) == true
    }
}
