package com.ecom.auth.entity;


import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;


@Document(collection = "refresh_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    private String id;


    @Indexed(unique = true)
    @Field("token")
    private String token;


    @Indexed
    @Field("user_id")
    private String userId;


    @Indexed(expireAfter = "0s")
    @Field("expires_at")
    private LocalDateTime expiresAt;


    @Field("device_info")
    private String deviceInfo;


    @Field("created_at")
    private LocalDateTime createdAt;
}
