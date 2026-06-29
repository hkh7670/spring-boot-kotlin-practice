package com.example.springbootkotlinpractice.common.converter

import com.example.springbootkotlinpractice.common.utils.AesCryptoUtil
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class Aes256Converter(
    private val aesCryptoUtil: AesCryptoUtil
) : AttributeConverter<String, String> {

    // 엔티티 → DB (저장 시 암호화)
    override fun convertToDatabaseColumn(attribute: String?): String? {
        return aesCryptoUtil.encrypt(attribute)
    }

    // DB → 엔티티 (조회 시 복호화)
    override fun convertToEntityAttribute(dbData: String?): String? {
        return aesCryptoUtil.decrypt(dbData)
    }
}
