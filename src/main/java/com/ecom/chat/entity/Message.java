package com.ecom.chat.entity;


import com.ecom.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;


@Document(collection = "messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message extends BaseEntity {

    @Indexed
    @Field("conversation_id")
    private String conversationId;

    @Field("sender_id")
    private String senderId;     // customer's userId, OR the literal "ADMIN"

    @Field("receiver_id")
    private String receiverId;   // the other side

    @Field("message")
    private String message;

    @Builder.Default
    @Field("read_status")
    private boolean readStatus = false;
}
