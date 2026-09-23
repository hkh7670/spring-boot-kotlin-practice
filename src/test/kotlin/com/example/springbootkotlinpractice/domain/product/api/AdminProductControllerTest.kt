package com.example.springbootkotlinpractice.domain.product.api

import com.example.springbootkotlinpractice.common.security.JwtTokenProvider
import com.example.springbootkotlinpractice.domain.category.entity.Category
import com.example.springbootkotlinpractice.domain.category.repository.CategoryRepository
import com.example.springbootkotlinpractice.domain.product.dto.AdminProductCreateRequest
import com.example.springbootkotlinpractice.domain.product.dto.AdminProductOptionRequest
import com.example.springbootkotlinpractice.domain.product.dto.AdminProductOptionUpdateRequest
import com.example.springbootkotlinpractice.domain.product.dto.AdminProductUpdateRequest
import com.example.springbootkotlinpractice.domain.product.entity.Product
import com.example.springbootkotlinpractice.domain.product.entity.ProductOption
import com.example.springbootkotlinpractice.domain.product.repository.ProductOptionRepository
import com.example.springbootkotlinpractice.domain.product.repository.ProductRepository
import com.example.springbootkotlinpractice.domain.search.document.ProductDocument
import com.example.springbootkotlinpractice.domain.search.repository.ProductSearchRepository
import com.example.springbootkotlinpractice.domain.vendor.entity.Vendor
import com.example.springbootkotlinpractice.domain.vendor.repository.VendorRepository
import com.example.springbootkotlinpractice.enums.CategoryLevel
import com.example.springbootkotlinpractice.enums.JoinProvider
import com.example.springbootkotlinpractice.enums.Role
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post

private const val BASE_URL = "/api/v1/admin/products"

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("관리자 상품 API 통합 테스트")
class AdminProductControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var productRepository: ProductRepository

    @Autowired
    lateinit var productOptionRepository: ProductOptionRepository

    @Autowired
    lateinit var categoryRepository: CategoryRepository

    @Autowired
    lateinit var vendorRepository: VendorRepository

    @Autowired
    lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    lateinit var productSearchRepository: ProductSearchRepository

    private lateinit var adminToken: String
    private lateinit var memberToken: String
    private lateinit var category: Category
    private lateinit var vendor: Vendor

    @BeforeEach
    fun setUp() {
        productOptionRepository.deleteAll()
        productRepository.deleteAll()
        categoryRepository.deleteAll()
        vendorRepository.deleteAll()

        adminToken = jwtTokenProvider.createAdminAccessToken(1L)
        memberToken = jwtTokenProvider.createAccessToken(
            1L, "member@example.com", JoinProvider.EMAIL, Role.USER
        )
        category = categoryRepository.save(
            Category.of(parentId = null, name = "테스트 카테고리", level = CategoryLevel.LARGE)
        )
        vendor = vendorRepository.save(Vendor.of(name = "테스트 업체"))
    }

    private fun createRequestBody(
        name: String = "신규 상품",
        categoryId: Long? = category.id,
        options: List<AdminProductOptionRequest> = listOf(
            AdminProductOptionRequest(name = "블랙", price = 10_000, stockCount = 5),
            AdminProductOptionRequest(name = "화이트", price = 11_000, stockCount = 3),
        ),
    ): String {
        return objectMapper.writeValueAsString(
            AdminProductCreateRequest(
                name = name,
                description = "상품 설명",
                imageUrl = "https://example.com/image.png",
                categoryId = categoryId,
                vendorId = vendor.id,
                productOptions = options,
            )
        )
    }

    private fun updateRequestBody(name: String = "수정된 상품"): String {
        return objectMapper.writeValueAsString(
            AdminProductUpdateRequest(
                name = name,
                description = "수정된 설명",
                imageUrl = null,
                categoryId = category.id,
                vendorId = null,
            )
        )
    }

    private fun bearer(token: String) = "Bearer $token"

    private fun saveProductWithOption(name: String = "기존 상품", optionName: String = "기본"): ProductOption {
        val product = productRepository.save(Product.of(name = name))
        return productOptionRepository.save(
            ProductOption.of(product = product, name = optionName, price = 10_000, stockCount = 5)
        )
    }

    @Test
    @DisplayName("토큰 없이 상품을 생성하면 401을 반환한다")
    fun `토큰 없이 상품을 생성하면 401을 반환한다`() {
        mockMvc.post(BASE_URL) {
            contentType = MediaType.APPLICATION_JSON
            content = createRequestBody()
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    @DisplayName("일반 회원 토큰으로 상품을 생성하면 403을 반환한다")
    fun `일반 회원 토큰으로 상품을 생성하면 403을 반환한다`() {
        mockMvc.post(BASE_URL) {
            header(HttpHeaders.AUTHORIZATION, bearer(memberToken))
            contentType = MediaType.APPLICATION_JSON
            content = createRequestBody()
        }.andExpect {
            status { isForbidden() }
        }

        assertThat(productRepository.count()).isZero()
    }

    @Test
    @DisplayName("관리자가 상품과 옵션을 함께 생성하고 검색 색인에 반영한다")
    fun `관리자가 상품과 옵션을 함께 생성하고 검색 색인에 반영한다`() {
        mockMvc.post(BASE_URL) {
            header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
            contentType = MediaType.APPLICATION_JSON
            content = createRequestBody()
        }.andExpect {
            status { isCreated() }
            jsonPath("$.data.name") { value("신규 상품") }
            jsonPath("$.data.categoryName") { value("테스트 카테고리") }
            jsonPath("$.data.vendorName") { value("테스트 업체") }
            jsonPath("$.data.productOptions.length()") { value(2) }
        }

        val product = productRepository.findAll().single()
        assertThat(product.isDeleted).isFalse()
        assertThat(productOptionRepository.findByProductId(product.id)).hasSize(2)
        verify(productSearchRepository).ensureIndexExists()
        verify(productSearchRepository).index(any())
    }

    @Test
    @DisplayName("검색 색인 갱신이 실패해도 상품 생성은 성공한다")
    fun `검색 색인 갱신이 실패해도 상품 생성은 성공한다`() {
        whenever(productSearchRepository.index(any())).thenThrow(RuntimeException("opensearch down"))

        mockMvc.post(BASE_URL) {
            header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
            contentType = MediaType.APPLICATION_JSON
            content = createRequestBody()
        }.andExpect {
            status { isCreated() }
        }

        assertThat(productRepository.count()).isEqualTo(1)
    }

    @Test
    @DisplayName("존재하지 않는 카테고리로 상품을 생성하면 404를 반환하고 저장하지 않는다")
    fun `존재하지 않는 카테고리로 상품을 생성하면 404를 반환하고 저장하지 않는다`() {
        mockMvc.post(BASE_URL) {
            header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
            contentType = MediaType.APPLICATION_JSON
            content = createRequestBody(categoryId = 999_999L)
        }.andExpect {
            status { isNotFound() }
        }

        assertThat(productRepository.count()).isZero()
        verify(productSearchRepository, never()).index(any())
    }

    @Test
    @DisplayName("옵션 없이 상품을 생성하면 400을 반환한다")
    fun `옵션 없이 상품을 생성하면 400을 반환한다`() {
        mockMvc.post(BASE_URL) {
            header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
            contentType = MediaType.APPLICATION_JSON
            content = createRequestBody(options = emptyList())
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    @DisplayName("요청 안에 옵션명이 중복되면 409를 반환한다")
    fun `요청 안에 옵션명이 중복되면 409를 반환한다`() {
        mockMvc.post(BASE_URL) {
            header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
            contentType = MediaType.APPLICATION_JSON
            content = createRequestBody(
                options = listOf(
                    AdminProductOptionRequest(name = "블랙", price = 10_000, stockCount = 1),
                    AdminProductOptionRequest(name = "블랙", price = 12_000, stockCount = 1),
                )
            )
        }.andExpect {
            status { isConflict() }
        }

        assertThat(productRepository.count()).isZero()
    }

    @Test
    @DisplayName("상품 정보를 수정하면 반영되고 검색 색인도 갱신한다")
    fun `상품 정보를 수정하면 반영되고 검색 색인도 갱신한다`() {
        val option = saveProductWithOption()
        val productId = option.product.id

        mockMvc.patch("$BASE_URL/$productId") {
            header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
            contentType = MediaType.APPLICATION_JSON
            content = updateRequestBody()
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.name") { value("수정된 상품") }
            jsonPath("$.data.description") { value("수정된 설명") }
        }

        assertThat(productRepository.findById(productId).get().name).isEqualTo("수정된 상품")
        val indexed = argumentCaptor<ProductDocument>()
        verify(productSearchRepository).index(indexed.capture())
        assertThat(indexed.firstValue.productId).isEqualTo(productId)
        assertThat(indexed.firstValue.name).isEqualTo("수정된 상품")
    }

    @Test
    @DisplayName("존재하지 않는 상품을 수정하면 404를 반환한다")
    fun `존재하지 않는 상품을 수정하면 404를 반환한다`() {
        mockMvc.patch("$BASE_URL/999999") {
            header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
            contentType = MediaType.APPLICATION_JSON
            content = updateRequestBody()
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    @DisplayName("상품을 삭제하면 soft delete 되어 공개 조회에서 사라지고 색인에서 제거된다")
    fun `상품을 삭제하면 soft delete 되어 공개 조회에서 사라지고 색인에서 제거된다`() {
        val option = saveProductWithOption()
        val productId = option.product.id

        mockMvc.delete("$BASE_URL/$productId") {
            header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
        }.andExpect {
            status { isOk() }
        }

        assertThat(productRepository.findById(productId).get().isDeleted).isTrue()
        assertThat(productOptionRepository.findById(option.id)).isPresent()
        mockMvc.get("/api/v1/products/$productId").andExpect {
            status { isNotFound() }
        }
        verify(productSearchRepository).delete(eq(productId))
    }

    @Test
    @DisplayName("이미 삭제된 상품을 다시 삭제하면 404를 반환한다")
    fun `이미 삭제된 상품을 다시 삭제하면 404를 반환한다`() {
        val productId = saveProductWithOption().product.id
        mockMvc.delete("$BASE_URL/$productId") {
            header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
        }.andExpect { status { isOk() } }

        mockMvc.delete("$BASE_URL/$productId") {
            header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    @DisplayName("상품에 옵션을 추가하면 상세 응답에 포함된다")
    fun `상품에 옵션을 추가하면 상세 응답에 포함된다`() {
        val productId = saveProductWithOption().product.id

        mockMvc.post("$BASE_URL/$productId/options") {
            header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                AdminProductOptionRequest(name = "신규 옵션", price = 15_000, stockCount = 7)
            )
        }.andExpect {
            status { isCreated() }
            jsonPath("$.data.productOptions.length()") { value(2) }
        }

        assertThat(productOptionRepository.findByProductId(productId).map { it.name })
            .containsExactlyInAnyOrder("기본", "신규 옵션")
    }

    @Test
    @DisplayName("이미 있는 옵션명으로 옵션을 추가하면 409를 반환한다")
    fun `이미 있는 옵션명으로 옵션을 추가하면 409를 반환한다`() {
        val productId = saveProductWithOption(optionName = "기본").product.id

        mockMvc.post("$BASE_URL/$productId/options") {
            header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                AdminProductOptionRequest(name = "기본", price = 15_000, stockCount = 7)
            )
        }.andExpect {
            status { isConflict() }
        }
    }

    @Test
    @DisplayName("옵션의 이름, 가격, 재고를 수정한다")
    fun `옵션의 이름 가격 재고를 수정한다`() {
        val option = saveProductWithOption()

        mockMvc.patch("$BASE_URL/${option.product.id}/options/${option.id}") {
            header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                AdminProductOptionUpdateRequest(name = "변경 옵션", price = 20_000, stockCount = 50)
            )
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.productOptions[0].name") { value("변경 옵션") }
        }

        val saved = productOptionRepository.findById(option.id).get()
        assertThat(saved.name).isEqualTo("변경 옵션")
        assertThat(saved.price).isEqualTo(20_000)
        assertThat(saved.stockCount).isEqualTo(50)
    }

    @Test
    @DisplayName("다른 상품의 옵션을 수정하려 하면 404를 반환한다")
    fun `다른 상품의 옵션을 수정하려 하면 404를 반환한다`() {
        val option = saveProductWithOption(name = "상품A")
        val otherProductId = saveProductWithOption(name = "상품B").product.id

        mockMvc.patch("$BASE_URL/$otherProductId/options/${option.id}") {
            header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                AdminProductOptionUpdateRequest(name = "변경", price = 20_000, stockCount = 50)
            )
        }.andExpect {
            status { isNotFound() }
        }

        assertThat(productOptionRepository.findById(option.id).get().name).isEqualTo("기본")
    }

    @Test
    @DisplayName("옵션 수정에서 재고를 생략하면 가격만 바뀌고 재고는 그대로다")
    fun `옵션 수정에서 재고를 생략하면 가격만 바뀌고 재고는 그대로다`() {
        val option = saveProductWithOption()

        mockMvc.patch("$BASE_URL/${option.product.id}/options/${option.id}") {
            header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                AdminProductOptionUpdateRequest(name = "기본", price = 20_000)
            )
        }.andExpect { status { isOk() } }

        val saved = productOptionRepository.findById(option.id).get()
        assertThat(saved.price).isEqualTo(20_000)
        assertThat(saved.stockCount).isEqualTo(5)
    }

    @Test
    @DisplayName("다른 옵션이 쓰는 이름으로 옵션명을 바꾸면 409를 반환한다")
    fun `다른 옵션이 쓰는 이름으로 옵션명을 바꾸면 409를 반환한다`() {
        val first = saveProductWithOption(optionName = "블랙")
        val second = productOptionRepository.save(
            ProductOption.of(product = first.product, name = "화이트", price = 10_000, stockCount = 5)
        )

        mockMvc.patch("$BASE_URL/${first.product.id}/options/${second.id}") {
            header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                AdminProductOptionUpdateRequest(name = "블랙", price = 10_000)
            )
        }.andExpect { status { isConflict() } }

        assertThat(productOptionRepository.findById(second.id).get().name).isEqualTo("화이트")
    }

    @Test
    @DisplayName("삭제된 상품에는 옵션을 추가하거나 수정할 수 없다")
    fun `삭제된 상품에는 옵션을 추가하거나 수정할 수 없다`() {
        val option = saveProductWithOption()
        val product = option.product
        product.delete()
        productRepository.save(product)

        mockMvc.post("$BASE_URL/${product.id}/options") {
            header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                AdminProductOptionRequest(name = "신규", price = 10_000, stockCount = 1)
            )
        }.andExpect { status { isNotFound() } }
        mockMvc.patch("$BASE_URL/${product.id}/options/${option.id}") {
            header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                AdminProductOptionUpdateRequest(name = "변경", price = 10_000)
            )
        }.andExpect { status { isNotFound() } }

        assertThat(productOptionRepository.findByProductId(product.id)).hasSize(1)
    }

    @Test
    @DisplayName("요청 안에서 대소문자만 다른 옵션명도 중복으로 보고 409를 반환한다")
    fun `요청 안에서 대소문자만 다른 옵션명도 중복으로 보고 409를 반환한다`() {
        mockMvc.post(BASE_URL) {
            header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
            contentType = MediaType.APPLICATION_JSON
            content = createRequestBody(
                options = listOf(
                    AdminProductOptionRequest(name = "Black", price = 10_000, stockCount = 1),
                    AdminProductOptionRequest(name = " black ", price = 10_000, stockCount = 1),
                )
            )
        }.andExpect { status { isConflict() } }

        assertThat(productRepository.count()).isZero()
    }

    @Test
    @DisplayName("옵션 가격이나 재고가 상한을 넘거나 옵션이 51개 이상이면 400을 반환한다")
    fun `옵션 가격이나 재고가 상한을 넘거나 옵션이 51개 이상이면 400을 반환한다`() {
        val tooExpensive = listOf(
            AdminProductOptionRequest(name = "고가", price = 100_000_001, stockCount = 1)
        )
        val tooMuchStock = listOf(
            AdminProductOptionRequest(name = "대량", price = 1_000, stockCount = 1_000_001)
        )
        val tooManyOptions = (1..51).map {
            AdminProductOptionRequest(name = "옵션$it", price = 1_000, stockCount = 1)
        }

        listOf(tooExpensive, tooMuchStock, tooManyOptions).forEach { options ->
            mockMvc.post(BASE_URL) {
                header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                contentType = MediaType.APPLICATION_JSON
                content = createRequestBody(options = options)
            }.andExpect { status { isBadRequest() } }
        }

        assertThat(productRepository.count()).isZero()
    }

    @Test
    @DisplayName("재색인은 삭제되지 않은 상품만 색인하고 삭제된 상품 문서는 인덱스에서 정리한다")
    fun `재색인은 삭제되지 않은 상품만 색인하고 삭제된 상품 문서는 인덱스에서 정리한다`() {
        val active = saveProductWithOption(name = "유지 상품").product
        val removed = saveProductWithOption(name = "삭제된 상품").product
        removed.delete()
        productRepository.save(removed)

        mockMvc.post("$BASE_URL/reindex") {
            header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
        }.andExpect { status { isOk() } }

        val indexed = argumentCaptor<List<ProductDocument>>()
        verify(productSearchRepository).bulkIndex(indexed.capture())
        assertThat(indexed.firstValue.map { it.productId }).containsExactly(active.id)
        verify(productSearchRepository).bulkDelete(eq(listOf(removed.id)))
    }
}
