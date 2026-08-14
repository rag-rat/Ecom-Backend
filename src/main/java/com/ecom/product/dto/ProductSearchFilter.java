package com.ecom.product.dto;

import lombok.Data;

import java.math.BigDecimal;


@Data
public class ProductSearchFilter {

    // Free-text search across name, description, tags (uses MongoDB $text)
    private String keyword;

    private String categoryId;

    private BigDecimal minPrice;
    private BigDecimal maxPrice;


    // Product Specific

//    private String fabric;
//    private String color;
//    private String occasion;

    //

    private String brand;
    private String color;
    private String productType;
    private String condition;


    private String sortBy;

    private String sortDir;

    private int page = 0;
    private int size = 20;


}
