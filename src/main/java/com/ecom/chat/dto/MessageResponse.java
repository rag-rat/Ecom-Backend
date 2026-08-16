package com.ecom.chat.dto;


import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Builder
public class MessageResponse {

    private String id;
    private String conversationId;
    private String senderId;
    private String receiverId;
    private String message;
    private boolean readStatus;
    private Instant createdAt;

}

