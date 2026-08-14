package com.ecom.inventory.entity;

import com.ecom.common.entity.BaseEntity;
import lombok.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;


@Document(collection = "inventory")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Inventory extends BaseEntity {


    @Indexed(unique = true)
    @Field("product_id")
    private String productId;

    @Builder.Default
    @Field("total_stock")
    private int totalStock = 0;

    @Builder.Default
    @Field("reserved_stock")
    private int reservedStock = 0;



    @Builder.Default
    @Field("low_stock_threshold")
    private int lowStockThreshold = 5;

    @Field("last_restocked_at")
    private LocalDateTime lastRestockedAt;




    public int getAvailableStock() {
        return Math.max(0, totalStock - reservedStock);
    }

    public boolean isLowStock() {
        return getAvailableStock() <= lowStockThreshold;
    }

    public boolean isInStock() {
        return getAvailableStock() > 0;
    }

}
