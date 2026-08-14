package com.ecom.admin.controller;

import com.ecom.common.dto.ApiResponse;
import com.ecom.inventory.dto.InventoryResponse;
import com.ecom.inventory.dto.RestockRequest;
import com.ecom.inventory.dto.UpdateLowStockThresholdRequest;
import com.ecom.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Inventory", description = "Stock management")
public class AdminInventoryController {

    private final InventoryService inventoryService;


    @GetMapping("/{productId}")
    @Operation(summary = "Get current stock levels for a product")
    public ResponseEntity<ApiResponse<InventoryResponse>> getInventory(
            @PathVariable String productId) {

        InventoryResponse response = inventoryService.getInventoryByProductId(productId);
        return ResponseEntity.ok(ApiResponse.success("Inventory fetched", response));
    }



    @PostMapping("/{productId}/restock")
    @Operation(summary = "Add newly received stock (quantity is additive)")
    public ResponseEntity<ApiResponse<InventoryResponse>> restock(
            @PathVariable String productId,
            @Valid @RequestBody RestockRequest request) {

        InventoryResponse response = inventoryService.restock(productId, request.getQuantity());
        return ResponseEntity.ok(ApiResponse.success("Stock updated successfully", response));
    }



    @PutMapping("/{productId}/low-stock-threshold")
    @Operation(summary = "Set the low-stock warning threshold for a product")
    public ResponseEntity<ApiResponse<InventoryResponse>> updateThreshold(
            @PathVariable String productId,
            @Valid @RequestBody UpdateLowStockThresholdRequest request) {

        InventoryResponse response =
                inventoryService.updateLowStockThreshold(productId, request.getThreshold());
        return ResponseEntity.ok(ApiResponse.success("Threshold updated", response));
    }
}
