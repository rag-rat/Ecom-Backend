package com.ecom.payment.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;



@Data
@Builder
public class CreatePaymentOrderResponse {

    private String razorpayOrderId;
    private BigDecimal amount;
    private String currency;
    private String keyId;  // Razorpay's PUBLIC key — safe to expose to frontend
    private String orderId; // our own order's ID, so the frontend can track context

}