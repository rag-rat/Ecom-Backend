package com.ecom.order.mapper;

import com.ecom.address.mapper.AddressMapper;
import com.ecom.order.dto.OrderResponse;
import com.ecom.order.entity.Order;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = AddressMapper.class)
public interface OrderMapper {
    OrderResponse toResponse(Order order);
}
