package com.ecom.order.dto;

import com.ecom.common.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class UpdateOrderStatusRequest {

    @NotNull(message = "Status is required")
    private OrderStatus status;
}
