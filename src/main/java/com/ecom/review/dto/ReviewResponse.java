package com.ecom.review.dto;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReviewResponse {

    private String id;
    private String productId;
    private String userName;
    private Integer rating;
    private String title;
    private String reviewText;
    private boolean verifiedPurchase;
    private int helpfulCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


    private boolean isOwner;

}