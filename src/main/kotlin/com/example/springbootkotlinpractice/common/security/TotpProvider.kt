package com.example.springbootkotlinpractice.common.security

import dev.samstevens.totp.code.DefaultCodeGenerator
import dev.samstevens.totp.code.DefaultCodeVerifier
import dev.samstevens.totp.code.HashingAlgorithm
import dev.samstevens.totp.qr.QrData
import dev.samstevens.totp.secret.DefaultSecretGenerator
import dev.samstevens.totp.time.SystemTimeProvider
import org.springframework.stereotype.Component

// TOTP(RFC 6238) 시크릿 생성/QR용 URI 생성/코드 검증만 담당하는 순수 컴포넌트.
// 시크릿의 DB 저장·조회, Redis pending 저장 등 상태 관리는 TotpService가 담당한다.
@Component
class TotpProvider {
    private val codeVerifier = DefaultCodeVerifier(DefaultCodeGenerator(), SystemTimeProvider())

    fun generateSecret(): String = DefaultSecretGenerator().generate()

    fun buildOtpAuthUri(email: String, secret: String): String {
        return QrData.Builder()
            .label(email)
            .secret(secret)
            .issuer(ISSUER)
            .algorithm(HashingAlgorithm.SHA1)
            .digits(6)
            .period(30)
            .build()
            .uri
    }

    fun verifyCode(secret: String, code: String): Boolean = codeVerifier.isValidCode(secret, code)

    companion object {
        private const val ISSUER = "spring-boot-kotlin-practice"
    }
}
