package com.ecom.product.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;


@Data
@Builder
public class ProductSummaryResponse {

    private String id;
    private String name;
    private BigDecimal price;
    private BigDecimal discountedPrice;
    private String primaryImageUrl;   // just the ONE thumbnail, not the full gallery
    private String categoryName;
    private double averageRating;
    private int reviewCount;
    private boolean featured;

}
