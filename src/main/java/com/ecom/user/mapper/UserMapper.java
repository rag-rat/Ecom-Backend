package com.ecom.user.mapper;


import com.ecom.user.dto.UserResponse;
import com.ecom.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "active", target = "active")
    UserResponse toResponse(User user);
}
