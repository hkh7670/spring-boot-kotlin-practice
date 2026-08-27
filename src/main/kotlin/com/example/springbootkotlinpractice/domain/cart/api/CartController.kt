package com.example.springbootkotlinpractice.domain.cart.api

import com.example.springbootkotlinpractice.common.dto.CommonResponse
import com.example.springbootkotlinpractice.common.dto.ResponseHandler
import com.example.springbootkotlinpractice.common.security.UserPrincipal
import com.example.springbootkotlinpractice.domain.cart.dto.CartItemAddRequest
import com.example.springbootkotlinpractice.domain.cart.dto.CartItemCountRequest
import com.example.springbootkotlinpractice.domain.cart.dto.CartResponse
import com.example.springbootkotlinpractice.domain.cart.service.CartService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "[CART] Cart", description = "회원별 장바구니 조회 / 담기 / 수량변경 / 삭제 API")
@RequestMapping("/api/v1/cart")
@RestController
class CartController(
    private val cartService: CartService,
) {

    @Operation(
        summary = "장바구니 조회 API",
        description = "본인 장바구니를 조회한다. 담긴 상품이 없으면 빈 배열을 반환한다. 재고 수량 대신 품절 여부(soldOut)만 노출한다.",
    )
    @PreAuthorize("hasRole('USER')")
    @GetMapping
    fun getCart(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
    ): ResponseEntity<CommonResponse<CartResponse>> {
        return ResponseHandler.ok(cartService.getCart(userPrincipal.id))
    }

    @Operation(
        summary = "장바구니 담기 API",
        description = "상품을 장바구니에 담는다. 이미 담긴 상품이면 수량을 더한다. 재고를 초과하면 담을 수 없다.",
    )
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/items")
    fun addItem(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @Valid @RequestBody request: CartItemAddRequest,
    ): ResponseEntity<CommonResponse<CartResponse>> {
        return ResponseHandler.ok(cartService.addItem(userPrincipal.id, request))
    }

    @Operation(
        summary = "장바구니 수량 변경 API",
        description = "장바구니에 담긴 상품의 수량을 변경한다. 재고를 초과하면 변경할 수 없다.",
    )
    @PreAuthorize("hasRole('USER')")
    @PatchMapping("/items/{productOptionId}")
    fun updateCount(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable productOptionId: Long,
        @Valid @RequestBody request: CartItemCountRequest,
    ): ResponseEntity<CommonResponse<CartResponse>> {
        return ResponseHandler.ok(cartService.updateCount(userPrincipal.id, productOptionId, request))
    }

    @Operation(
        summary = "장바구니 삭제 API",
        description = "장바구니에서 상품을 제거한다. 이미 없는 상품이어도 성공으로 처리한다.",
    )
    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/items/{productOptionId}")
    fun removeItem(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable productOptionId: Long,
    ): ResponseEntity<CommonResponse<CartResponse>> {
        return ResponseHandler.ok(cartService.removeItem(userPrincipal.id, productOptionId))
    }
}
