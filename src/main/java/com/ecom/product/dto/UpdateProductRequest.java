package com.ecom.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;


@Data
public class UpdateProductRequest {

    @Size(min = 3, max = 150, message = "Name must be between 3 and 150 characters")
    private String name;

    @Size(max = 3000, message = "Description cannot exceed 3000 characters")
    private String description;

    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    private BigDecimal discountedPrice;


    // product specific

    private String categoryId;
//    private String fabric;
//    private String color;
//    private String occasion;

    //

    private String brand;
    private String color;
    private String productType;
    private String condition;


    private String length;
    private List<String> tags;
    private Boolean featured;
    private Boolean active;
}
