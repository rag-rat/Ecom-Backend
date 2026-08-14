package com.ecom.review.entity;

import com.ecom.common.entity.BaseEntity;
import lombok.*;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;


@Document(collection = "reviews")
//@CompoundIndexes({
//        @CompoundIndex(name = "product_user_unique",
//                def = "{'productId': 1, 'userId': 1}", unique = true),
//        @CompoundIndex(name = "product_date_idx",
//                def = "{'productId': 1, 'createdAt': -1}")
//})
@CompoundIndexes({
        @CompoundIndex(
                name = "product_user_unique",
                def = "{'productId': 1, 'userId': 1}",
                unique = true,
                partialFilter = "{ 'isActive': true }"
        ),
        @CompoundIndex(
                name = "product_date_idx",
                def = "{'productId': 1, 'createdAt': -1}"
        )
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review extends BaseEntity {

    @Field("product_id")
    private String productId;

    @Field("user_id")
    private String userId;

    @Field("user_name")
    private String userName;

    @Field("rating")
    private Integer rating;

    @Field("title")
    private String title;

    @Field("review_text")
    private String reviewText;

    @Builder.Default
    @Field("verified_purchase")
    private boolean verifiedPurchase = false;

    @Builder.Default
    @Field("is_active")
    private boolean active = true;

    @Builder.Default
    @Field("helpful_count")
    private int helpfulCount = 0;

}