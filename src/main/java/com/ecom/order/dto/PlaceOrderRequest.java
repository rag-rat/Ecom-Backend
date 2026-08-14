package com.ecom.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;


@Data
public class PlaceOrderRequest {

    @NotBlank(message = "Shipping address is required")
    private String addressId;

    // Optional delivery instructions from the customer
    private String notes;

}