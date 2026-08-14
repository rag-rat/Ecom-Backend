package com.ecom.address.entity;

import com.ecom.common.entity.BaseEntity;
import lombok.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;


@Document(collection = "addresses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address extends BaseEntity {

    @Indexed
    @Field("user_id")
    private String userId;

    @Field("full_name")
    private String fullName;

    @Field("phone_number")
    private String phoneNumber;

    @Field("address_line1")
    private String addressLine1;

    @Field("address_line2")
    private String addressLine2;  // optional — apartment/landmark

    @Field("city")
    private String city;

    @Field("state")
    private String state;

    @Field("pincode")
    private String pincode;

    @Builder.Default
    @Field("country")
    private String country = "India";

    @Builder.Default
    @Field("is_default")
    private boolean defaultIs = false;

}