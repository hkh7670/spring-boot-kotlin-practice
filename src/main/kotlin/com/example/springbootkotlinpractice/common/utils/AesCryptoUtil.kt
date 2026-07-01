package com.example.springbootkotlinpractice.common.utils

import com.example.springbootkotlinpractice.common.config.AesProperties
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

@Component
class AesCryptoUtil(
    aesProperties: AesProperties
) {
    companion object {
        private const val AES_CIPHER_TRANSFORMATION = "AES/CBC/PKCS5Padding"
    }

    private val keySpec = SecretKeySpec(
        Base64.getDecoder().decode(aesProperties.secret),
        "AES"
    )
    private val ivParameterSpec = IvParameterSpec(
        Base64.getDecoder().decode(aesProperties.initVector)
    )

    fun encrypt(plainText: String?): String? {
        if (plainText.isNullOrEmpty()) return plainText
        return try {
            val cipher = Cipher.getInstance(AES_CIPHER_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivParameterSpec)
            Base64.getEncoder().encodeToString(cipher.doFinal(plainText.toByteArray()))
        } catch (e: Exception) {
            throw RuntimeException("Encryption failed", e)
        }
    }

    fun decrypt(cipherText: String?): String? {
        if (cipherText.isNullOrEmpty()) return cipherText
        return try {
            val cipher = Cipher.getInstance(AES_CIPHER_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivParameterSpec)
            String(cipher.doFinal(Base64.getDecoder().decode(cipherText)))
        } catch (e: Exception) {
            throw RuntimeException("Decryption failed", e)
        }
    }
}