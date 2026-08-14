package com.ecom.chat.controller;


import com.ecom.chat.dto.MessageResponse;
import com.ecom.chat.service.ChatService;
import com.ecom.common.dto.ApiResponse;
import com.ecom.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;



@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Customer chat history")
public class CustomerChatHistoryController {

    private final ChatService chatService;
    private final com.ecom.chat.repository.ConversationRepository conversationRepository;


    // getMyHistory()

    @GetMapping("/history")
    @Operation(summary = "Get the current user's conversation history")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getMyHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        String customerId = userDetails.getUsername();

        var conversation = conversationRepository.findByCustomerId(customerId);

        if (conversation.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("No conversation yet", List.of()));
        }

        chatService.markAsReadByCustomer(customerId);

        List<MessageResponse> history =
                chatService.getConversationHistory(conversation.get().getId(), page, size);

        return ResponseEntity.ok(ApiResponse.success("Chat history fetched", history));
    }
}