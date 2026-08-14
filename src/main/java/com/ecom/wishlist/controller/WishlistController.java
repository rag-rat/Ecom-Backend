package com.ecom.wishlist.controller;

import com.ecom.common.dto.ApiResponse;
import com.ecom.security.CustomUserDetails;
import com.ecom.wishlist.dto.WishlistResponse;
import com.ecom.wishlist.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
@Tag(name = "Wishlist", description = "Saved products for later")
public class WishlistController {

    private final WishlistService wishlistService;


    @GetMapping
    @Operation(summary = "Get the current user's wishlist")
    public ResponseEntity<ApiResponse<WishlistResponse>> getWishlist(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        WishlistResponse response = wishlistService.getWishlist(userDetails.getUsername());

        return ResponseEntity.ok(ApiResponse.success("Wishlist fetched", response));
    }


    @PostMapping("/{productId}")
    @Operation(summary = "Add a product to the wishlist")
    public ResponseEntity<ApiResponse<WishlistResponse>> addProduct(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String productId) {

        WishlistResponse response = wishlistService.addProduct(userDetails.getUsername(), productId);

        return ResponseEntity.ok(ApiResponse.success("Product added to wishlist", response));
    }


    @DeleteMapping("/{productId}")
    @Operation(summary = "Remove a product from the wishlist")
    public ResponseEntity<ApiResponse<WishlistResponse>> removeProduct(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String productId) {

        WishlistResponse response = wishlistService.removeProduct(userDetails.getUsername(), productId);

        return ResponseEntity.ok(ApiResponse.success("Product removed from wishlist", response));
    }

}
