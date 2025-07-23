package com.goodda.jejuday.pay.controller;

import com.goodda.jejuday.pay.dto.ProductDto;
import com.goodda.jejuday.pay.entity.ProductCategory;
import com.goodda.jejuday.pay.service.ProductService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping("/{productId}/exchange")
    public ResponseEntity<String> exchangeProduct(@PathVariable Long productId,
                                                  @RequestParam Long userId) {
        productService.exchangeProduct(userId, productId);
        return ResponseEntity.ok("상품 교환 완료");
    }

    @GetMapping
    public ResponseEntity<List<ProductDto>> getProducts(@RequestParam ProductCategory category) {
        return ResponseEntity.ok(productService.getProductsByCategory(category));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductDto> getProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(productService.getProduct(productId));
    }
}
