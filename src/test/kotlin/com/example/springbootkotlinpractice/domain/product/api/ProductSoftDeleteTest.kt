package com.example.springbootkotlinpractice.domain.product.api

import com.example.springbootkotlinpractice.common.security.JwtTokenProvider
import com.example.springbootkotlinpractice.domain.cart.dto.CartItemAddRequest
import com.example.springbootkotlinpractice.domain.cart.entity.CartItem
import com.example.springbootkotlinpractice.domain.cart.repository.CartItemRepository
import com.example.springbootkotlinpractice.domain.delivery.entity.DeliveryOption
import com.example.springbootkotlinpractice.domain.delivery.repository.DeliveryOptionRepository
import com.example.springbootkotlinpractice.domain.member.entity.Member
import com.example.springbootkotlinpractice.domain.member.repository.MemberRepository
import com.example.springbootkotlinpractice.domain.order.dto.OrderCreateRequest
import com.example.springbootkotlinpractice.domain.order.dto.OrderItemRequest
import com.example.springbootkotlinpractice.domain.order.repository.OrderItemRepository
import com.example.springbootkotlinpractice.domain.order.repository.OrderRepository
import com.example.springbootkotlinpractice.domain.payment.repository.PaymentRepository
import com.example.springbootkotlinpractice.domain.product.entity.Product
import com.example.springbootkotlinpractice.domain.product.entity.ProductOption
import com.example.springbootkotlinpractice.domain.product.repository.ProductOptionRepository
import com.example.springbootkotlinpractice.domain.product.repository.ProductRepository
import com.example.springbootkotlinpractice.enums.JoinProvider
import com.fasterxml.jackson.databind.ObjectMapper
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("상품 soft delete 공개 API 반영 통합 테스트")
class ProductSoftDeleteTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var memberRepository: MemberRepository

    @Autowired
    lateinit var productRepository: ProductRepository

    @Autowired
    lateinit var productOptionRepository: ProductOptionRepository

    @Autowired
    lateinit var deliveryOptionRepository: DeliveryOptionRepository

    @Autowired
    lateinit var cartItemRepository: CartItemRepository

    @Autowired
    lateinit var orderRepository: OrderRepository

    @Autowired
    lateinit var orderItemRepository: OrderItemRepository

    @Autowired
    lateinit var paymentRepository: PaymentRepository

    @Autowired
    lateinit var jwtTokenProvider: JwtTokenProvider

    private lateinit var member: Member
    private lateinit var accessToken: String
    private lateinit var activeProduct: Product
    private lateinit var targetProduct: Product
    private lateinit var targetOption: ProductOption
    private lateinit var deliveryOption: DeliveryOption

    @BeforeEach
    fun setUp() {
        cartItemRepository.deleteAll()
        orderItemRepository.deleteAll()
        paymentRepository.deleteAll()
        orderRepository.deleteAll()
        productOptionRepository.deleteAll()
        productRepository.deleteAll()
        deliveryOptionRepository.deleteAll()
        memberRepository.deleteAll()

        member = memberRepository.save(
            Member.of(
                lastName = "한",
                firstName = "규호",
                birthDate = LocalDate.of(1998, 5, 20),
                phoneNumber = "010-1234-5678",
                email = "soft-delete-test@example.com",
                password = "encoded-password",
                joinProvider = JoinProvider.EMAIL,
            )
        )
        accessToken = jwtTokenProvider.createAccessToken(
            member.id, member.email, member.joinProvider, member.role
        )

        activeProduct = productRepository.save(Product.of(name = "유지 상품"))
        productOptionRepository.save(
            ProductOption.of(product = activeProduct, name = "기본", price = 5_000, stockCount = 10)
        )
        targetProduct = productRepository.save(Product.of(name = "삭제 대상 상품"))
        targetOption = productOptionRepository.save(
            ProductOption.of(product = targetProduct, name = "기본", price = 10_000, stockCount = 5)
        )
        deliveryOption = deliveryOptionRepository.save(DeliveryOption(name = "기본 배송", price = 3_000))
    }

    private fun authHeader() = "Bearer $accessToken"

    private fun softDeleteTargetProduct() {
        targetProduct.delete()
        productRepository.save(targetProduct)
    }

    private fun orderRequestBody(count: Int): String {
        return objectMapper.writeValueAsString(
            OrderCreateRequest(
                deliveryOptionId = deliveryOption.id,
                items = listOf(OrderItemRequest(productOptionId = targetOption.id, count = count)),
            )
        )
    }

    @Test
    @DisplayName("삭제된 상품은 상품 목록에서 제외된다")
    fun `삭제된 상품은 상품 목록에서 제외된다`() {
        softDeleteTargetProduct()

        mockMvc.get("/api/v1/products").andExpect {
            status { isOk() }
            jsonPath("$.data.totalElements") { value(1) }
            jsonPath("$.data.content[0].name") { value("유지 상품") }
        }
    }

    @Test
    @DisplayName("검색어로 조회해도 삭제된 상품은 결과에 나오지 않는다")
    fun `검색어로 조회해도 삭제된 상품은 결과에 나오지 않는다`() {
        softDeleteTargetProduct()

        mockMvc.get("/api/v1/products") {
            param("keyword", "삭제 대상")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.totalElements") { value(0) }
        }
    }

    @Test
    @DisplayName("삭제된 상품의 상세 조회는 404를 반환한다")
    fun `삭제된 상품의 상세 조회는 404를 반환한다`() {
        softDeleteTargetProduct()

        mockMvc.get("/api/v1/products/${targetProduct.id}").andExpect {
            status { isNotFound() }
        }
    }

    @Test
    @DisplayName("삭제된 상품의 옵션으로 주문 생성 시 404를 반환하고 재고는 그대로다")
    fun `삭제된 상품의 옵션으로 주문 생성 시 404를 반환하고 재고는 그대로다`() {
        softDeleteTargetProduct()

        mockMvc.post("/api/v1/orders") {
            header(HttpHeaders.AUTHORIZATION, authHeader())
            contentType = MediaType.APPLICATION_JSON
            content = orderRequestBody(1)
        }.andExpect {
            status { isNotFound() }
        }

        assertThat(productOptionRepository.findById(targetOption.id).get().stockCount).isEqualTo(5)
    }

    @Test
    @DisplayName("삭제된 상품의 옵션은 장바구니에 담을 수 없다")
    fun `삭제된 상품의 옵션은 장바구니에 담을 수 없다`() {
        softDeleteTargetProduct()

        mockMvc.post("/api/v1/cart/items") {
            header(HttpHeaders.AUTHORIZATION, authHeader())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                CartItemAddRequest(productOptionId = targetOption.id, count = 1)
            )
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    @DisplayName("이미 장바구니에 담긴 상품이 삭제되면 장바구니 조회에서 제외된다")
    fun `이미 장바구니에 담긴 상품이 삭제되면 장바구니 조회에서 제외된다`() {
        cartItemRepository.save(CartItem.of(member.id, targetOption.id, 1))
        softDeleteTargetProduct()

        mockMvc.get("/api/v1/cart") {
            header(HttpHeaders.AUTHORIZATION, authHeader())
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.items.length()") { value(0) }
        }
    }

    @Test
    @DisplayName("상품이 삭제되어도 과거 주문 상세에는 상품 정보가 그대로 조회된다")
    fun `상품이 삭제되어도 과거 주문 상세에는 상품 정보가 그대로 조회된다`() {
        val orderId = mockMvc.post("/api/v1/orders") {
            header(HttpHeaders.AUTHORIZATION, authHeader())
            contentType = MediaType.APPLICATION_JSON
            content = orderRequestBody(1)
        }.andExpect {
            status { isCreated() }
        }.andReturn().let {
            objectMapper.readTree(it.response.contentAsString).path("data").path("orderId").asLong()
        }

        softDeleteTargetProduct()

        mockMvc.get("/api/v1/orders/$orderId") {
            header(HttpHeaders.AUTHORIZATION, authHeader())
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.itemList[0].productName") { value("삭제 대상 상품") }
        }
    }
}
