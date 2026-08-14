package com.ecom.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;


@Data
public class CreatePaymentOrderRequest {

    @NotBlank(message = "Order ID is required")
    private String orderId;
}