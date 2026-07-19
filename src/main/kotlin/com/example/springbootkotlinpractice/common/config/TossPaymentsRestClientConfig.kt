package com.example.springbootkotlinpractice.common.config

import com.example.springbootkotlinpractice.common.Logging
import com.example.springbootkotlinpractice.common.payment.toss.TossPaymentsApi
import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.exception.ApiErrorException
import java.nio.charset.StandardCharsets
import java.util.Base64
import org.apache.hc.client5.http.config.ConnectionConfig
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager
import org.apache.hc.core5.util.TimeValue
import org.apache.hc.core5.util.Timeout
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpRequest
import org.springframework.http.HttpStatusCode
import org.springframework.http.client.ClientHttpResponse
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestClient
import org.springframework.web.client.support.RestClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory
import org.springframework.web.service.invoker.createClient

@Configuration
class TossPaymentsRestClientConfig(
    private val tossPaymentsProperties: TossPaymentsProperties,
) : Logging {

    companion object {
        private const val MAX_TOTAL_CONNECTIONS = 50
        private const val MAX_PER_ROUTE = 20
        private const val MAX_RETRY_ATTEMPT_COUNT = 3
        private const val RETRY_INTERVAL_SECONDS = 1L
        private const val MAX_IDLE_TIME_SECONDS = 30L
        private const val CONNECT_TIMEOUT_SECONDS = 3L
        private const val RESPONSE_TIMEOUT_SECONDS = 5L
        private const val TOSS_PAYMENTS_BASE_URL = "https://api.tosspayments.com"
    }

    @Bean
    fun tossPaymentsApi(): TossPaymentsApi {
        val restClient = RestClient.builder()
            .baseUrl(TOSS_PAYMENTS_BASE_URL)
            .requestFactory(HttpComponentsClientHttpRequestFactory(createApacheHttpClient()))
            .defaultHeader(HttpHeaders.AUTHORIZATION, buildBasicAuthHeader())
            .defaultStatusHandler({ status: HttpStatusCode -> status.is4xxClientError }, tossErrorHandler())
            .defaultStatusHandler({ status: HttpStatusCode -> status.is5xxServerError }, tossErrorHandler())
            .build()
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
            .build()
            .createClient<TossPaymentsApi>()
    }

    private fun buildBasicAuthHeader(): String {
        val credentials = "${tossPaymentsProperties.secretKey}:"
        return "Basic " + Base64.getEncoder().encodeToString(credentials.toByteArray(StandardCharsets.UTF_8))
    }

    private fun tossErrorHandler(): RestClient.ResponseSpec.ErrorHandler {
        return RestClient.ResponseSpec.ErrorHandler { request, response ->
            logExternalError(request, response)
            throw ApiErrorException(ResponseCodeEnum.PAYMENT_CONFIRM_FAILED)
        }
    }

    private fun logExternalError(request: HttpRequest, response: ClientHttpResponse) {
        val responseBody = response.body.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        logger.error("[Toss Payments Request] {} {}", request.method, request.uri)
        logger.error("[Toss Payments Response] Status Code: {}", response.statusCode.value())
        logger.error("[Toss Payments Response] Body: {}", responseBody)
    }

    private fun createApacheHttpClient(): CloseableHttpClient {
        return HttpClients.custom()
            .setConnectionManager(createConnectionManager())
            .setDefaultRequestConfig(createRequestConfig())
            .setRetryStrategy(createRetryStrategy())
            .evictIdleConnections(Timeout.ofSeconds(MAX_IDLE_TIME_SECONDS))
            .build()
    }

    private fun createConnectionManager(): PoolingHttpClientConnectionManager {
        val connectionConfig = ConnectionConfig.custom()
            .setConnectTimeout(Timeout.ofSeconds(CONNECT_TIMEOUT_SECONDS))
            .setSocketTimeout(Timeout.ofSeconds(RESPONSE_TIMEOUT_SECONDS))
            .build()
        return PoolingHttpClientConnectionManager().apply {
            maxTotal = MAX_TOTAL_CONNECTIONS
            defaultMaxPerRoute = MAX_PER_ROUTE
            setDefaultConnectionConfig(connectionConfig)
        }
    }

    private fun createRequestConfig(): RequestConfig {
        return RequestConfig.custom()
            .setResponseTimeout(Timeout.ofSeconds(RESPONSE_TIMEOUT_SECONDS))
            .build()
    }

    private fun createRetryStrategy(): DefaultHttpRequestRetryStrategy {
        return DefaultHttpRequestRetryStrategy(MAX_RETRY_ATTEMPT_COUNT, TimeValue.ofSeconds(RETRY_INTERVAL_SECONDS))
    }
}
