package com.example.springbootkotlinpractice.common.config

import com.example.springbootkotlinpractice.common.Logging
import com.example.springbootkotlinpractice.common.oauth.GoogleOAuthApi
import com.example.springbootkotlinpractice.common.oauth.GoogleTokenApi
import com.example.springbootkotlinpractice.common.oauth.KakaoOAuthApi
import com.example.springbootkotlinpractice.common.oauth.KakaoTokenApi
import com.example.springbootkotlinpractice.common.oauth.NaverOAuthApi
import com.example.springbootkotlinpractice.common.oauth.NaverTokenApi
import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.exception.ApiErrorException
import org.apache.hc.client5.http.config.ConnectionConfig
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager
import org.apache.hc.core5.util.TimeValue
import org.apache.hc.core5.util.Timeout
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpRequest
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.ClientHttpResponse
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestClient
import org.springframework.web.client.support.RestClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory
import java.nio.charset.StandardCharsets

@Configuration
class OAuthRestClientConfig: Logging {

    companion object {

        private const val MAX_TOTAL_CONNECTIONS = 100
        private const val MAX_PER_ROUTE = 20
        private const val MAX_RETRY_ATTEMPT_COUNT = 3
        private const val RETRY_INTERVAL_SECONDS = 1L
        private const val MAX_IDLE_TIME_SECONDS = 30L
        private const val CONNECT_TIMEOUT_SECONDS = 3L
        private const val RESPONSE_TIMEOUT_SECONDS = 5L
    }

    // OAuth Provider(Google/Kakao/Naver) 호출 시 Apache HttpClient5 커넥션 풀을 공유해서 사용한다
    @Bean
    fun oAuthClientHttpRequestFactory(): ClientHttpRequestFactory {
        return HttpComponentsClientHttpRequestFactory(createApacheHttpClient())
    }

    @Bean
    fun googleOAuthApi(requestFactory: ClientHttpRequestFactory): GoogleOAuthApi {
        return createOAuthApi(requestFactory, "https://www.googleapis.com", GoogleOAuthApi::class.java)
    }

    // Authorization Code + PKCE 교환 전용 (userinfo 와 호스트가 다름)
    @Bean
    fun googleTokenApi(requestFactory: ClientHttpRequestFactory): GoogleTokenApi {
        return createOAuthApi(requestFactory, "https://oauth2.googleapis.com", GoogleTokenApi::class.java)
    }

    @Bean
    fun kakaoOAuthApi(requestFactory: ClientHttpRequestFactory): KakaoOAuthApi {
        return createOAuthApi(requestFactory, "https://kapi.kakao.com", KakaoOAuthApi::class.java)
    }

    // Authorization Code + PKCE 교환 전용 (userinfo 와 호스트가 다름)
    @Bean
    fun kakaoTokenApi(requestFactory: ClientHttpRequestFactory): KakaoTokenApi {
        return createOAuthApi(requestFactory, "https://kauth.kakao.com", KakaoTokenApi::class.java)
    }

    @Bean
    fun naverOAuthApi(requestFactory: ClientHttpRequestFactory): NaverOAuthApi {
        return createOAuthApi(requestFactory, "https://openapi.naver.com", NaverOAuthApi::class.java)
    }

    // Authorization Code + PKCE 교환 전용 (userinfo 와 호스트가 다름)
    @Bean
    fun naverTokenApi(requestFactory: ClientHttpRequestFactory): NaverTokenApi {
        return createOAuthApi(requestFactory, "https://nid.naver.com", NaverTokenApi::class.java)
    }

    private fun <T> createOAuthApi(
        requestFactory: ClientHttpRequestFactory,
        baseUrl: String,
        apiClass: Class<T>,
    ): T {
        val restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(requestFactory)
            .defaultStatusHandler({ status: HttpStatusCode -> status.is4xxClientError }, oAuth4xxErrorHandler())
            .defaultStatusHandler({ status: HttpStatusCode -> status.is5xxServerError }, oAuth5xxErrorHandler())
            .build()
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
            .build()
            .createClient(apiClass)
    }

    private fun oAuth4xxErrorHandler(): RestClient.ResponseSpec.ErrorHandler {
        return RestClient.ResponseSpec.ErrorHandler { request, response ->
            logExternalError(request, response)
            if (response.statusCode == HttpStatus.UNAUTHORIZED) {
                throw ApiErrorException(ResponseCodeEnum.INVALID_OAUTH_TOKEN)
            }
            throw ApiErrorException(ResponseCodeEnum.EXTERNAL_SERVER_ERROR)
        }
    }

    private fun oAuth5xxErrorHandler(): RestClient.ResponseSpec.ErrorHandler {
        return RestClient.ResponseSpec.ErrorHandler { request, response ->
            logExternalError(request, response)
            throw ApiErrorException(ResponseCodeEnum.EXTERNAL_SERVER_ERROR)
        }
    }

    private fun logExternalError(request: HttpRequest, response: ClientHttpResponse) {
        val responseBody = response.body.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        logger.error("[External Request] {} {}", request.method, request.uri)
        logger.error("[External Response] Status Code: {}", response.statusCode.value())
        logger.error("[External Response] Body: {}", responseBody)
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
