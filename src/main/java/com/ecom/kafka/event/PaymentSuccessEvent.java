package com.ecom.kafka.event;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSuccessEvent {

    private String orderId;
    private String orderNumber;
    private String userId;
    private String customerEmail;
    private String customerName;
    private List<PaymentItem> items;
    private BigDecimal amountPaid;
    private String razorpayPaymentId;


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentItem {
        private String productId;
        private String productName;
        private int quantity;
    }
}