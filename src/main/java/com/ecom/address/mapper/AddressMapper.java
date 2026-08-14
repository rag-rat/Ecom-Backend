package com.ecom.address.mapper;

import com.ecom.address.dto.AddressResponse;
import com.ecom.address.entity.Address;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AddressMapper {

    AddressResponse toResponse(Address address);

    List<AddressResponse> toResponseList(List<Address> addresses);

}