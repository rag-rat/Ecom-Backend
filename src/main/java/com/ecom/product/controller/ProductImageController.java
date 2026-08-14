package com.ecom.product.controller;

import com.ecom.common.dto.ApiResponse;
import com.ecom.product.dto.ProductResponse;
import com.ecom.product.entity.Product;
import com.ecom.product.service.ProductImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;



@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Product Images", description = "Upload and manage product photos")
public class ProductImageController {

    private final ProductImageService productImageService;



    // uploadImage()

    @PostMapping(value = "/{productId}/images", consumes = "multipart/form-data")
    @Operation(summary = "Upload a product image (admin)")
    public ResponseEntity<ApiResponse<ProductResponse>> uploadImage(
            @PathVariable String productId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean isPrimary) {

        ProductResponse response =
                productImageService.uploadProductImage(productId, file, isPrimary);
        return ResponseEntity.ok(ApiResponse.success("Image uploaded successfully", response));
    }


    // deleteImage()

    @DeleteMapping("/{productId}/images")
    @Operation(summary = "Remove a product image (admin)")
    public ResponseEntity<ApiResponse<ProductResponse>> deleteImage(
            @PathVariable String productId,
            @RequestParam String publicId) {

        ProductResponse response =
                productImageService.deleteProductImage(productId, publicId);
        return ResponseEntity.ok(ApiResponse.success("Image removed successfully", response));
    }


    // setPrimaryImage()

    @PutMapping("/{productId}/images/set-primary")
    @Operation(summary = "Set an existing image as the primary thumbnail (admin)")
    public ResponseEntity<ApiResponse<ProductResponse>> setPrimaryImage(
            @PathVariable String productId,
            @RequestParam String publicId) {

        ProductResponse response =
                productImageService.setPrimaryImage(productId, publicId);
        return ResponseEntity.ok(ApiResponse.success("Primary image updated", response));
    }

}
