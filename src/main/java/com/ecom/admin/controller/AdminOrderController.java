package com.ecom.admin.controller;

import com.ecom.common.dto.ApiResponse;
import com.ecom.common.dto.PageResponse;
import com.ecom.common.enums.OrderStatus;
import com.ecom.order.dto.OrderResponse;
import com.ecom.order.dto.UpdateOrderStatusRequest;
import com.ecom.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Orders", description = "Order management and fulfillment")
public class AdminOrderController {

    private final OrderService orderService;


    // getAllOrders()

    @GetMapping
    @Operation(summary = "View all orders across all customers")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var response = orderService.getAllOrdersForAdmin(page, size);

        return ResponseEntity.ok(ApiResponse.success("Orders fetched", response));
    }


    // getOrdersByStatus()

    @GetMapping("/status/{status}")
    @Operation(summary = "Filter orders by status")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getOrdersByStatus(
            @PathVariable OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var response = orderService.getOrdersByStatusForAdmin(status, page, size);

        return ResponseEntity.ok(ApiResponse.success("Orders fetched", response));
    }


    // getOrderById()

    @GetMapping("/{orderId}")
    @Operation(summary = "View any order's details")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @PathVariable String orderId) {

        OrderResponse response = orderService.getOrderByIdForAdmin(orderId);

        return ResponseEntity.ok(ApiResponse.success("Order fetched", response));
    }


    // updateStatus()

    @PutMapping("/{orderId}/status")
    @Operation(summary = "Update order status (forward-only state machine)")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable String orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {

        OrderResponse response =
                orderService.updateOrderStatus(orderId, request.getStatus());

        return ResponseEntity.ok(ApiResponse.success("Order status updated", response));
    }

}
