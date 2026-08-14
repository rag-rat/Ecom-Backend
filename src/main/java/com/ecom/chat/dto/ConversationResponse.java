package com.ecom.chat.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;


@Data
@Builder
public class ConversationResponse {

    private String id;
    private String customerId;
    private String customerName;
    private String status;
    private String lastMessagePreview;
    private LocalDateTime lastMessageAt;
    private int unreadCount; // resolved to whichever side is viewing — see service

}