package com.ecom.review.dto;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ProductRatingsSummary {

    private String productId;
    private double averageRating;
    private long totalRatings;
    private long totalReviews;
    private Map<Integer, Long> ratingBreakdown;

}