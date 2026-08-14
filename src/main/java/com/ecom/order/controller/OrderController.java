package com.ecom.order.controller;

import com.ecom.common.dto.ApiResponse;
import com.ecom.common.dto.PageResponse;
import com.ecom.order.dto.CancelOrderRequest;
import com.ecom.order.dto.OrderResponse;
import com.ecom.order.dto.PlaceOrderRequest;
import com.ecom.order.service.OrderService;
import com.ecom.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order placement and history")
public class OrderController {

    private final OrderService orderService;


    // placeOrder()

    @PostMapping
    @Operation(summary = "Place an order from the current cart")
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PlaceOrderRequest request) {

        OrderResponse response = orderService.placeOrder(userDetails.getUsername(), request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order placed successfully", response));
    }


    // getMyOrders()

    @GetMapping
    @Operation(summary = "Get the current user's order history")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        var response = orderService.getMyOrders(userDetails.getUsername(), page, size);

        return ResponseEntity.ok(ApiResponse.success("Orders fetched", response));
    }



    // getOrderById()

    @GetMapping("/{orderId}")
    @Operation(summary = "Get a specific order's details")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String orderId) {

        OrderResponse response = orderService.getOrderById(userDetails.getUsername(), orderId);

        return ResponseEntity.ok(ApiResponse.success("Order fetched", response));
    }



    // cancelOrder()

    @PutMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel an order (only while PENDING or CONFIRMED)")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String orderId,
            @Valid @RequestBody CancelOrderRequest request) {

        OrderResponse response =
                orderService.cancelOrder(userDetails.getUsername(), orderId, request);

        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully", response));
    }


}
