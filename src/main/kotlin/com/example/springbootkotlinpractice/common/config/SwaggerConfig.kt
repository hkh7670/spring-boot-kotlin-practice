package com.example.springbootkotlinpractice.common.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig(
    @Value($$"${app.swagger.server-url:}")
    private val swaggerServerUrl: String? = null,
//    @Value($$"${spring.profiles.active:}")
//    private val profile: String,
) {
    companion object {
        private const val SECURITY_SCHEME_NAME = "bearerAuth"
    }

    @Bean
    fun openAPI(): OpenAPI {
        val openAPI = OpenAPI()
            .info(apiInfo())
            .addSecurityItem(SecurityRequirement().addList(SECURITY_SCHEME_NAME))
            .components(
                Components()
                    .addSecuritySchemes(SECURITY_SCHEME_NAME, bearerSecurityScheme())
            )

        if (swaggerServerUrl?.isNotBlank() == true) {
            openAPI.servers(
                listOf<Server?>(
                    Server().url(swaggerServerUrl),
                    Server().url("http://localhost:8080"),
                )
            )
        }

        return openAPI
    }

    private fun apiInfo(): Info? {
        return Info()
            .title("Spring Boot API")
            .description("Spring Boot API")
            .version("v1")
    }

    private fun bearerSecurityScheme(): SecurityScheme? {
        return SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .`in`(SecurityScheme.In.HEADER)
            .name("Authorization")
    }

}
