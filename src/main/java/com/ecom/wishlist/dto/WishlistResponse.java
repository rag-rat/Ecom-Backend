package com.ecom.wishlist.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;


@Data
@Builder
public class WishlistResponse {

    private String userId;
    private List<com.ecom.product.dto.ProductSummaryResponse> products;
    private int totalItems;

}