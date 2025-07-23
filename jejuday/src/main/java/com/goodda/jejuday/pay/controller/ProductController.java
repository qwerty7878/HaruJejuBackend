package com.goodda.jejuday.pay.controller;

import com.goodda.jejuday.auth.dto.ApiResponse;
import com.goodda.jejuday.pay.dto.ProductDto;
import com.goodda.jejuday.pay.entity.ProductCategory;
import com.goodda.jejuday.pay.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/products")
@RequiredArgsConstructor
@Tag(name = "상품 API", description = "상품 조회 및 교환 관련 API")
public class ProductController {

    private final ProductService productService;

    @PostMapping("/{productId}/exchange")
    @Operation(summary = "상품 교환", description = "사용자가 보유한 포인트로 상품을 교환합니다.")
    public ResponseEntity<ApiResponse<String>> exchangeProduct(
            @Parameter(description = "상품 ID") @PathVariable Long productId,
            @Parameter(description = "사용자 ID") @RequestParam Long userId) {
        productService.exchangeProduct(userId, productId);
        return ResponseEntity.ok(ApiResponse.onSuccess("상품 교환 완료"));
    }

    @GetMapping
    @Operation(summary = "상품 목록 조회", description = "카테고리에 따른 상품 리스트를 조회합니다.")
    public ResponseEntity<ApiResponse<List<ProductDto>>> getProducts(
            @Parameter(description = "상품 카테고리") @RequestParam ProductCategory category) {
        List<ProductDto> products = productService.getProductsByCategory(category);
        return ResponseEntity.ok(ApiResponse.onSuccess(products));
    }

    @GetMapping("/{productId}")
    @Operation(summary = "단일 상품 조회", description = "상품 ID로 특정 상품 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<ProductDto>> getProduct(
            @Parameter(description = "상품 ID") @PathVariable Long productId) {
        ProductDto product = productService.getProduct(productId);
        return ResponseEntity.ok(ApiResponse.onSuccess(product));
    }
}
