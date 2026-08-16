package com.ecom.chat.entity;

import com.ecom.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.time.LocalDateTime;


@Document(collection = "conversations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation extends BaseEntity {

    @Indexed(unique = true)
    @Field("customer_id")
    private String customerId;


    @Field("customer_name")
    private String customerName; // denormalized, same reasoning as Review.userName


    @Builder.Default
    @Field("status")
    private ConversationStatus status = ConversationStatus.OPEN;


    @Field("last_message_preview")
    private String lastMessagePreview;

//    @Field("last_message_at")
//    private LocalDateTime lastMessageAt;


    @Field("last_message_at")
    private Instant lastMessageAt;


    /*
     * Unread count is tracked SEPARATELY for each side, since "unread"
     * is relative to who's looking. unreadByAdmin increments when the
     * customer sends a message; unreadByCustomer increments when admin
     * replies. Each resets to 0 when that side views the conversation.
     */
    @Builder.Default
    @Field("unread_by_admin")
    private int unreadByAdmin = 0;


    @Builder.Default
    @Field("unread_by_customer")
    private int unreadByCustomer = 0;


    public enum ConversationStatus {
        OPEN, CLOSED
    }

}