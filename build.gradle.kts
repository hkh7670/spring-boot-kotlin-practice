plugins {
    val kotlinVersion = "2.4.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    kotlin("plugin.jpa") version kotlinVersion
    kotlin("kapt") version kotlinVersion
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

group = "com.example"
//version = "0.0.1-SNAPSHOT"
description = "spring-boot-kotlin-practice"

tasks.jar {
    enabled = false
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

val querydslVersion = "7.6"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    // Boot 4 기본값은 Jackson 3(신규 tools.jackson.* 스택)이라 classic ObjectMapper 빈이 없음 —
    // Jackson 3 전면 마이그레이션은 범위 밖이라 공식 가이드의 "임시 Jackson 2 유지" 경로를 택함
    implementation("org.springframework.boot:spring-boot-jackson2")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // JWT (jjwt 0.12.x)
    implementation("io.jsonwebtoken:jjwt-api:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")

    // OpenFeign QueryDSL 포크 (Jakarta / Spring Boot 3.x)
    implementation("io.github.openfeign.querydsl:querydsl-jpa:$querydslVersion")
    kapt("io.github.openfeign.querydsl:querydsl-apt:$querydslVersion:jakarta")

    runtimeOnly("com.h2database:h2")
    // H2 콘솔 자동설정이 Boot 4에서 별도 모듈로 분리됨(PathRequest.toH2Console()이 런타임에 필요)
    runtimeOnly("org.springframework.boot:spring-boot-h2console")
    runtimeOnly("com.mysql:mysql-connector-j")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-webmvc-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    /* Swagger */
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.0")

    /* Spring Security */
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    testImplementation("org.springframework.security:spring-security-test")

    /* ULID Creator */
    implementation("com.github.f4b6a3:ulid-creator:5.2.4")

    /* OAuth Provider 호출용 HTTP Client */
    implementation("org.apache.httpcomponents.client5:httpclient5")

    /* Redis */
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    /* Kafka */
    implementation("org.springframework.boot:spring-boot-starter-kafka")

    /* TOTP (2단계 인증, RFC 6238) */
    implementation("dev.samstevens.totp:totp:1.7.1")

    /* OpenSearch (상품명 자동완성) — 트랜스포트는 내장 ApacheHttpClient5TransportBuilder가
       위 httpclient5 의존성을 그대로 사용하므로 별도 transport 아티팩트 불필요 */
    implementation("org.opensearch.client:opensearch-java:3.9.0")

    /* Kotlin Logging */
//    runtimeOnly("io.github.oshai:kotlin-logging-jvm:8.0.4")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
