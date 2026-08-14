package com.ecom.cart.mapper;

import com.ecom.cart.dto.CartResponse;
import com.ecom.cart.entity.Cart;
import org.mapstruct.Mapper;


@Mapper(componentModel = "spring")
public interface CartMapper {

    CartResponse toResponse(Cart cart);

}