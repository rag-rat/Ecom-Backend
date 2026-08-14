package com.ecom.inventory.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;


@Data
@Builder
public class InventoryResponse {
    private String productId;
    private int totalStock;
    private int reservedStock;
    private int availableStock;
    private int lowStockThreshold;
    private boolean lowStock;
    private boolean inStock;
    private LocalDateTime lastRestockedAt;
}
