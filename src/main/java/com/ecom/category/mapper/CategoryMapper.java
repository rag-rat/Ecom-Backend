package com.ecom.category.mapper;


import com.ecom.category.dto.CategoryResponse;
import com.ecom.category.entity.Category;
import org.mapstruct.Mapper;


@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryResponse toResponse(Category category);

}