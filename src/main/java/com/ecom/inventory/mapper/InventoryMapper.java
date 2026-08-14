package com.ecom.inventory.mapper;

import com.ecom.inventory.dto.InventoryResponse;
import com.ecom.inventory.entity.Inventory;
import org.mapstruct.Mapping;
import org.mapstruct.Mapper;


@Mapper(componentModel = "spring")
public interface InventoryMapper {

    @Mapping(target = "availableStock", expression = "java(inventory.getAvailableStock())")
    @Mapping(target = "lowStock", expression = "java(inventory.isLowStock())")
    @Mapping(target = "inStock", expression = "java(inventory.isInStock())")
    InventoryResponse toResponse(Inventory inventory);

}
