package com.example.springbootkotlinpractice.exception

import org.springframework.validation.FieldError

data class ErrorField private constructor(
    val fieldName: String?,   // 에러가 발생한 필드 명
    val fieldValue: Any?,     // 에러가 발생한 필드에 할당되어있는 값
    val message: String?,     // 에러 메시지
) {
    companion object {

        fun from(fieldError: FieldError): ErrorField {
            return ErrorField(
                fieldName = fieldError.field,
                fieldValue = fieldError.rejectedValue,
                message = resolveMessage(fieldError),
            )
        }

        fun fromInvalidFormat(fieldName: String?, rejectedValue: Any?, targetType: Class<*>?): ErrorField {
            val message = if (targetType != null) {
                resolveTypeMessage(targetType)
            } else {
                "데이터 형식이 올바르지 않습니다."
            }

            return ErrorField(
                fieldName = fieldName,
                fieldValue = rejectedValue,
                message = message,
            )
        }

        fun of(fieldName: String?, fieldValue: Any?, message: String?): ErrorField {
            return ErrorField(
                fieldName = fieldName,
                fieldValue = fieldValue,
                message = message,
            )
        }

        private fun resolveMessage(fieldError: FieldError): String? {
            val codes = fieldError.codes ?: emptyArray()
            val isTypeMismatch = codes.any { it.startsWith("typeMismatch") }

            if (!isTypeMismatch) {
                return fieldError.defaultMessage
            }

            return extractQualifiedTypeName(codes)
                ?.let { name -> runCatching { Class.forName(name) }.getOrNull() }
                ?.let { resolveTypeMessage(it) }
                ?: "데이터 형식이 올바르지 않습니다."
        }

        private fun resolveTypeMessage(targetType: Class<*>): String {
            return when (targetType.simpleName) {
                "LocalDate" -> "날짜 형식이 올바르지 않습니다. (yyyy-MM-dd)"
                "LocalDateTime" -> "날짜/시간 형식이 올바르지 않습니다. (yyyy-MM-dd'T'HH:mm:ss)"
                "LocalTime" -> "시간 형식이 올바르지 않습니다. (HH:mm:ss)"
                "Integer", "Long", "Double", "Float", "BigDecimal" -> "숫자를 입력해주세요."
                "Boolean" -> "올바른 값을 입력해주세요. (true/false)"
                else -> if (targetType.isEnum) buildEnumMessage(targetType) else "데이터 형식이 올바르지 않습니다."
            }
        }

        private fun buildEnumMessage(enumClass: Class<*>): String {
            val validValues = enumClass.enumConstants.joinToString(", ") { it.toString() }
            return "올바른 값을 입력해주세요. ($validValues)"
        }

        private fun extractQualifiedTypeName(codes: Array<String>): String? {
            return codes.asSequence()
                .filter { it.startsWith("typeMismatch.") }
                .map { it.substring("typeMismatch.".length) }
                .filter { it.contains(".") }
                .firstOrNull { code ->
                    val simpleName = code.substringAfterLast('.')
                    simpleName.isNotEmpty() && simpleName[0].isUpperCase()
                }
        }
    }
}
