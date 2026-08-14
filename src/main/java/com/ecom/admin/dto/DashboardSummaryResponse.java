package com.ecom.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


@Data
@Builder
public class DashboardSummaryResponse {

    private BigDecimal totalRevenue;          // sum of CONFIRMED+ orders' totalAmount

    private long totalOrders;

    private Map<String, Long> ordersByStatus; // {"PENDING": 5, "CONFIRMED": 12, ...}

    private long totalCustomers;

    private long totalProducts;

    private List<LowStockAlert> lowStockProducts;
    private List<TopProduct> topSellingProducts;


    @Data
    @Builder
    public static class LowStockAlert {
        private String productId;
        private String productName;
        private int availableStock;
        private int threshold;
    }


    @Data
    @Builder
    public static class TopProduct {
        private String productId;
        private String productName;
        private int totalUnitsSold;
    }

}
