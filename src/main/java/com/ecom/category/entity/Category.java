package com.ecom.category.entity;


import com.ecom.common.entity.BaseEntity;
import lombok.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;


@Document(collection = "categories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category extends BaseEntity {

    @Indexed(unique = true)
    @Field("name")
    private String name;


    @Field("description")
    private String description;


    @Field("image_url")
    private String imageUrl;


    @Builder.Default
    @Field("is_active")
    private boolean active = true;


    @Builder.Default
    @Field("display_order")
    private int displayOrder = 0;

}
