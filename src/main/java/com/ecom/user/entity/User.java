package com.ecom.user.entity;


import com.ecom.common.entity.BaseEntity;
import com.ecom.common.enums.RoleType;
import lombok.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;


@Document(collection = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    @Indexed(unique = true)
    @Field("email")
    private String email;

    @Field("password_hash")
    private String passwordHash;

    @Field("first_name")
    private String firstName;

    @Field("last_name")
    private String lastName;

    @Field("phone_number")
    private String phoneNumber;

    @Builder.Default
    @Field("role")
    private RoleType role = RoleType.CUSTOMER;


    @Builder.Default
    @Field("is_active")
    private boolean isActive = true;

    @Field("profile_picture_url")
    private String profilePictureUrl;



    // oauth

    @Field("provider")
    private String provider; // "local", "google", "github"

    @Field("provider_id")
    private String providerId;
}
