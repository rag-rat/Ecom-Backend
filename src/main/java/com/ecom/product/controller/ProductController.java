package com.ecom.product.controller;

import com.ecom.common.dto.ApiResponse;
import com.ecom.common.dto.PageResponse;
import com.ecom.product.dto.*;
import com.ecom.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;


@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Browse, search, filter, and manage products")
public class ProductController {

    private final ProductService productService;


    @GetMapping
    @Operation(summary = "Search and filter products (public)")
    public ResponseEntity<ApiResponse<PageResponse<ProductSummaryResponse>>> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
//            @RequestParam(required = false) String fabric,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String color,
//            @RequestParam(required = false) String occasion,
            @RequestParam(required = false) String productType,
            @RequestParam(required = false) String condition,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {

        ProductSearchFilter filter = new ProductSearchFilter();
        filter.setKeyword(keyword);
        filter.setCategoryId(categoryId);
        filter.setMinPrice(minPrice);
        filter.setMaxPrice(maxPrice);
//        filter.setFabric(fabric);
        filter.setBrand(brand);
        filter.setColor(color);
//        filter.setOccasion(occasion);
        filter.setProductType(productType);
        filter.setCondition(condition);
        filter.setSortBy(sortBy);
        filter.setSortDir(sortDir);
        filter.setPage(page);
        filter.setSize(size);


//        System.out.println(minPrice.getClass().getName());
//        System.out.println(maxPrice.getClass().getName());

        var response = productService.searchProducts(filter);
        return ResponseEntity.ok(ApiResponse.success("Products fetched", response));

    }



    @GetMapping("/featured")
    @Operation(summary = "Get featrued products for homepage (public)")
    public ResponseEntity<ApiResponse<PageResponse<ProductSummaryResponse>>> getFeaturedProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        var response = productService.getFeaturedProducts(page, size);
        return ResponseEntity.ok(ApiResponse.success("Featured products fetched", response));
    }



    @GetMapping("/{productId}")
    @Operation(summary = "Get full product detail (public)")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(
            @PathVariable String productId) {

        ProductResponse response = productService.getProductById(productId);
        return ResponseEntity.ok(ApiResponse.success("Product fetched", response));
    }


    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new product (admin)")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody CreateProductRequest request) {

        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", response));
    }



    @PutMapping("/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a product (admin, partial update)")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable String productId,
            @Valid @RequestBody UpdateProductRequest request) {

        ProductResponse response = productService.updateProduct(productId, request);
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", response));
    }



    @DeleteMapping("/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft-delete a product (admin)")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable String productId) {

        productService.deleteProduct(productId);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully"));
    }

}
