package com.ecom.review.dto;
import jakarta.validation.constraints.*;
import lombok.Data;


@Data
public class CreateReviewRequest {

    @NotBlank(message = "Product ID is required")
    private String productId;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot exceed 5")
    private Integer rating;

    @Size(max = 100, message = "Title cannot exceed 100 characters")
    private String title;

    @Size(max = 2000, message = "Review text cannot exceed 2000 characters")
    private String reviewText;

}
