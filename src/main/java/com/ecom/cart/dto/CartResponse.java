package com.ecom.cart.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;


@Data
@Builder
public class CartResponse {

    private String userId;

    private List<CartItemResponse> items;

    private BigDecimal totalAmount;

    private int totalItems;


    @Data
    @Builder
    public static class CartItemResponse {

        private String productId;
        private String productName;
        private String productImage;
        private BigDecimal price;
        private int quantity;
        private BigDecimal subtotal;
        private boolean inStock;
    }
}
