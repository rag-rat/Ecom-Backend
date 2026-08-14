package com.ecom.product.entity;

import com.ecom.common.entity.BaseEntity;
import lombok.*;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.util.List;



@Document(collection = "products")
@CompoundIndexes({
        @CompoundIndex(name = "category_active_price_idx",
                def = "{'categoryId': 1, 'isActive': 1, 'price': 1}"),
        @CompoundIndex(name = "active_featured_idx",
                def = "{'isActive': 1, 'isFeatured': 1}")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product extends BaseEntity {

    @TextIndexed(weight = 3)   // Highest weight — name matches matter most
    @Field("name")
    private String name;


    @TextIndexed(weight = 1)   // Lower weight — description matches matter less
    @Field("description")
    private String description;


    @Field(value = "price", targetType = FieldType.DECIMAL128)
    private BigDecimal price;

    @Field(value = "discounted_price", targetType = FieldType.DECIMAL128)
    private BigDecimal discountedPrice;


    @Field("category_id")
    private String categoryId;


    @Field("category_name")
    private String categoryName;


    @Field("images")
    private List<ProductImage> images;


    // Product Specific

//    @Field("fabric")
//    private String fabric;       // e.g. "Silk", "Cotton", "Georgette"
//
//
//    @Field("color")
//    private String color;
//
//
//    @Field("occasion")
//    private String occasion;
//
//
//    @Field("length")
//    private String length;

    //



    @Field("brand")
    private String brand;       // e.g. "Apple", "Dell", "HP"

    @Field("color")
    private String color;       // e.g. "Black", "Silver", "White"

    @Field("productType")
    private String productType; // e.g. "Laptop", "Smartphone", "Monitor"

    @Field("condition")
    private String condition;   // e.g. "New", "Refurbished", "Used"


    @TextIndexed(weight = 2)
    @Field("tags")
    private List<String> tags;


    @Builder.Default
    @Field("is_active")
    private boolean active = true;


    @Builder.Default
    @Field("is_featured")
    private boolean featured = false;


    @Builder.Default
    @Field("average_rating")
    private double averageRating = 0.0;


    @Builder.Default
    @Field("review_count")
    private int reviewCount = 0;



    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductImage {
        private String url;        // Cloudinary secure URL
        private String publicId;   // Cloudinary public_id — needed to delete the image later
        private boolean primary; // Shown as the thumbnail in listings
    }

}
