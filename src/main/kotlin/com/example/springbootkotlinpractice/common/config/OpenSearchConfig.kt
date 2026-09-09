package com.example.springbootkotlinpractice.common.config

import org.apache.hc.core5.http.HttpHost
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.transport.OpenSearchTransport
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(OpenSearchProperties::class)
class OpenSearchConfig(
    private val openSearchProperties: OpenSearchProperties,
) {
    @Bean(destroyMethod = "close")
    fun openSearchTransport(): OpenSearchTransport {
        val httpHost = HttpHost(
            openSearchProperties.scheme,
            openSearchProperties.host,
            openSearchProperties.port,
        )
        return ApacheHttpClient5TransportBuilder.builder(httpHost).build()
    }

    @Bean
    fun openSearchClient(openSearchTransport: OpenSearchTransport): OpenSearchClient {
        return OpenSearchClient(openSearchTransport)
    }
}
