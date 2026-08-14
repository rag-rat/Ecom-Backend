package com.ecom.wishlist.entity;

import com.ecom.common.entity.BaseEntity;
import lombok.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.ArrayList;
import java.util.List;



@Document(collection = "wishlists")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wishlist extends BaseEntity {

    @Indexed(unique = true)
    @Field("user_id")
    private String userId;

    @Builder.Default
    @Field("product_ids")
    private List<String> productIds = new ArrayList<>();
}
