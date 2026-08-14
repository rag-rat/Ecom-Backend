package com.ecom.chat.controller;

import com.ecom.chat.dto.ChatMessageRequest;
import com.ecom.chat.dto.MessageResponse;
import com.ecom.chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;


@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;


    // handleCustomerMessage()

    @MessageMapping("/chat.send")
    public void handleCustomerMessage(
            @Payload ChatMessageRequest request,
            Authentication authentication) {

        if (authentication == null) {
            log.warn("Rejected chat message from unauthenticated WebSocket session");
            return;
        }

        String customerId = authentication.getName();

        MessageResponse response = chatService.sendCustomerMessage(customerId, request.getContent());

        log.info("Routed customer message: conversationId={}", response.getConversationId());
    }
}
