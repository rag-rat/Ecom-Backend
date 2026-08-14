package com.ecom.product.mapper;

import com.ecom.product.dto.ProductResponse;
import com.ecom.product.dto.ProductSummaryResponse;
import com.ecom.product.entity.Product;
import org.mapstruct.Mapping;
import org.mapstruct.Mapper;

import java.util.List;


@Mapper(componentModel = "spring")
public interface ProductMapper {

    ProductResponse toResponse(Product product);

    @Mapping(target = "primaryImageUrl", expression = "java(extractPrimaryImageUrl(product))")
    ProductSummaryResponse toSummaryResponse(Product product);


    default String extractPrimaryImageUrl(Product product) {
        if (product.getImages() == null || product.getImages().isEmpty()) {
            return null;
        }
        return product.getImages().stream()
                .filter(Product.ProductImage::isPrimary)
                .findFirst()
                .map(Product.ProductImage::getUrl)
                .orElse(product.getImages().get(0).getUrl()); // fallback to first image
    }
}