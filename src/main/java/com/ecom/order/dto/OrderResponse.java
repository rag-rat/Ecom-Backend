package com.ecom.order.dto;

import com.ecom.address.dto.AddressResponse;
import com.ecom.order.entity.Order;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderResponse {

    private String id;
    private String orderNumber;
    private List<Order.OrderItem> items;
    private AddressResponse shippingAddress;
    private String status;
    private BigDecimal subtotal;
    private BigDecimal shippingCharge;
    private BigDecimal discount;
    private BigDecimal totalAmount;
    private String paymentStatus;
    private String cancelReason;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;

}

