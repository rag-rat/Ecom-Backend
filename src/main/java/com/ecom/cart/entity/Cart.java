package com.ecom.cart.entity;

import com.ecom.common.entity.BaseEntity;
import lombok.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


@Document(collection = "carts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cart extends BaseEntity {

    @Indexed(unique = true)
    @Field("user_id")
    private String userId;

    @Builder.Default
    @Field("items")
    private List<CartItem> items = new ArrayList<>();


    @Builder.Default
    @Field("total_amount")
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Builder.Default
    @Field("total_items")
    private int totalItems = 0;


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItem {
        private String productId;
        private String productName;     // snapshot at add-time
        private String productImage;    // snapshot at add-time (primary image URL)
        private BigDecimal price;       // snapshot at add-time
        private int quantity;
        private BigDecimal subtotal;    // price * quantity, recalculated on every change
    }

}
