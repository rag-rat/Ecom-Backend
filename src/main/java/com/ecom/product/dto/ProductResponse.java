package com.ecom.product.dto;

import com.ecom.product.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private String id;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal discountedPrice;
    private String categoryId;
    private String categoryName;
    private List<Product.ProductImage> images;

    // product specific
//    private String fabric;
//    private String color;
//    private String occasion;
//    private String length;

    //


    private String brand;
    private String color;
    private String productType;
    private String condition;

    private List<String> tags;
    private boolean active;
    private boolean featured;
    private double averageRating;
    private int reviewCount;

}
