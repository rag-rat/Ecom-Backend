package com.ecom.cart.controller;

import com.ecom.cart.dto.AddToCartRequest;
import com.ecom.cart.dto.CartResponse;
import com.ecom.cart.dto.UpdateCartItemRequest;
import com.ecom.cart.service.CartService;
import com.ecom.common.dto.ApiResponse;
import com.ecom.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping cart management")
public class CartController {

    private final CartService cartService;


    // Get User Cart

    @GetMapping
    @Operation(summary = "Get the current user's cart")
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        CartResponse response = cartService.getCart(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Cart fetched", response));
    }



    // Add Product to the Cart

    @PostMapping("/items")
    @Operation(summary = "Add a product to the cart")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AddToCartRequest request) {

        CartResponse response = cartService.addToCart(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Item added to cart", response));
    }



    // Update Quantity of a Cart Item

    @PutMapping("/items/{productId}")
    @Operation(summary = "Update quantity of a specific cart item")
    public ResponseEntity<ApiResponse<CartResponse>> updateQuantity(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String productId,
            @Valid @RequestBody UpdateCartItemRequest request) {

        CartResponse response = cartService.updateQuantity(
                userDetails.getUsername(), productId, request.getQuantity());
        return ResponseEntity.ok(ApiResponse.success("Cart item updated", response));
    }



    // Delete a Product from Cart

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Remove a specific product from the cart")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String productId) {

        CartResponse response = cartService.removeItem(userDetails.getUsername(), productId);
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart", response));
    }



    // Delete Entire Cart

    @DeleteMapping
    @Operation(summary = "Clear the entire cart")
    public ResponseEntity<ApiResponse<CartResponse>> clearCart(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        CartResponse response = cartService.clearCart(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Cart cleared", response));
    }

}
