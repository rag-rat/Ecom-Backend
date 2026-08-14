package com.ecom.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {

    private String orderId;
    private String orderNumber;
    private String userId;
    private String customerEmail;
    private String customerName;
    private List<OrderItemSummary> items;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemSummary {
        private String productName;
        private int quantity;
    }
}