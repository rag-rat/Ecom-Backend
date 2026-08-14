package com.ecom.product.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;


// Input DTO for admin product creation.

@Data
@Builder
public class CreateProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(min = 3, max = 150, message = "Name must be between 3 and 150 characters")
    private String name;


    @NotBlank(message = "Description is required")
    @Size(max = 3000, message = "Description cannot exceed 3000 characters")
    private String description;


    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;


    @DecimalMin(value = "0.0", inclusive = false, message = "Discounted price must be greater than 0")
    private BigDecimal discountedPrice;


    @NotBlank(message = "Category ID is required")
    private String categoryId;


    // Product Specific

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


    @Builder.Default
    private boolean featured = false;


    @NotNull(message = "Initial stock quantity is required")
    @Min(value = 0, message = "Initial stock cannot be negative")
    private Integer initialStock;


}
