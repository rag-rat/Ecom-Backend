package com.ecom.admin.controller;

import com.ecom.chat.dto.ChatMessageRequest;
import com.ecom.chat.dto.ConversationResponse;
import com.ecom.chat.dto.MessageResponse;
import com.ecom.chat.repository.ConversationRepository;
import com.ecom.chat.service.ChatService;
import com.ecom.common.dto.ApiResponse;
import com.ecom.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;



@RestController
@RequestMapping("/api/admin/chat")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Chat", description = "Customer support conversation management")
public class AdminChatController {

    private final ChatService chatService;
    private final ConversationRepository conversationRepository;


    // getAllConversations()

    @GetMapping("/conversations")
    @Operation(summary = "List all conversations, most recent first")
    public ResponseEntity<ApiResponse<PageResponse<ConversationResponse>>> getAllConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);

        var conversationPage = conversationRepository.findAllByOrderByLastMessageAtDesc(pageable);

        List<ConversationResponse> responses = conversationPage.getContent().stream()
                .map(conv -> ConversationResponse.builder()
                        .id(conv.getId())
                        .customerId(conv.getCustomerId())
                        .customerName(conv.getCustomerName())
                        .status(conv.getStatus().name())
                        .lastMessagePreview(conv.getLastMessagePreview())
                        .lastMessageAt(conv.getLastMessageAt())
                        .unreadCount(conv.getUnreadByAdmin()) // admin's perspective
                        .build())
                .toList();

        var response = PageResponse.of(
                responses,
                conversationPage.getNumber(),
                conversationPage.getSize(),
                conversationPage.getTotalElements(),
                conversationPage.getTotalPages()
        );

        return ResponseEntity.ok(ApiResponse.success("Conversations fetched", response));
    }



    // getHistory() --- get Conversation messages History

    @GetMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "Get message history for a conversation")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getHistory(
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        chatService.markAsReadByAdmin(conversationId);

        List<MessageResponse> history =
                chatService.getConversationHistory(conversationId, page, size);

        return ResponseEntity.ok(ApiResponse.success("Messages fetched", history));
    }



    // reply()  --- admin reply to a conversation or customer

    @PostMapping("/conversations/{conversationId}/reply")
    @Operation(summary = "Send a reply to a customer")
    public ResponseEntity<ApiResponse<MessageResponse>> reply(
            @PathVariable String conversationId,
            @Valid @RequestBody ChatMessageRequest request) {

        MessageResponse response = chatService.sendAdminReply(conversationId, request.getContent());

        return ResponseEntity.ok(ApiResponse.success("Reply sent", response));
    }



    // close() --- admin close a conversation

    @PutMapping("/conversations/{conversationId}/close")
    @Operation(summary = "Close a conversation")
    public ResponseEntity<ApiResponse<Void>> close(@PathVariable String conversationId) {

        chatService.closeConversation(conversationId);

        return ResponseEntity.ok(ApiResponse.success("Conversation closed"));
    }



    // reopen() --- admin reopen a conversation

    @PutMapping("/conversations/{conversationId}/reopen")
    @Operation(summary = "Reopen a closed conversation")
    public ResponseEntity<ApiResponse<Void>> reopen(@PathVariable String conversationId) {

        chatService.reopenConversation(conversationId);

        return ResponseEntity.ok(ApiResponse.success("Conversation reopened"));
    }


    @PutMapping("/conversations/{conversationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable String conversationId) {

        chatService.markAsReadByAdmin(conversationId);

        return ResponseEntity.ok(ApiResponse.success("Conversation marked as read"));
    }

}
