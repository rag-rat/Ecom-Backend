package com.ecom.category.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;


@Data
public class UpdateCategoryRequest {

    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    private String imageUrl;

    private Integer displayOrder;

    private Boolean active;

}
