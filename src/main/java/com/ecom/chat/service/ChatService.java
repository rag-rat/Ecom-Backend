package com.ecom.chat.service;

import com.ecom.chat.dto.MessageResponse;
import com.ecom.chat.entity.Conversation;
import com.ecom.chat.entity.Message;
import com.ecom.chat.repository.ConversationRepository;
import com.ecom.chat.repository.MessageRepository;
import com.ecom.exception.ResourceNotFoundException;
import com.ecom.user.entity.User;
import com.ecom.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private static final String ADMIN_ID = "ADMIN";



    // sendCustomerMessage()

    public MessageResponse sendCustomerMessage(String customerId, String content) {

        Conversation conversation = conversationRepository.findByCustomerId(customerId)
                .orElseGet(() -> createNewConversation(customerId));


        // If a customer messages into a CLOSED conversation, reopen it —
        // the customer reaching back out is itself a reasonable signal
        // that support attention is needed again.
        if (conversation.getStatus() == Conversation.ConversationStatus.CLOSED) {
            conversation.setStatus(Conversation.ConversationStatus.OPEN);
        }


        Message message = Message.builder()
                .conversationId(conversation.getId())
                .senderId(customerId)
                .receiverId(ADMIN_ID)
                .message(content)
                .readStatus(false)
                .build();


        Message saved = messageRepository.save(message);


        conversation.setLastMessagePreview(truncate(content));
        conversation.setLastMessageAt(Instant.now());
        conversation.setUnreadByAdmin(conversation.getUnreadByAdmin() + 1);
        conversationRepository.save(conversation);

        MessageResponse response = toResponse(saved);

        /*
         * PUSH TO ADMIN: we broadcast to a fixed "ADMIN" destination
         * rather than a specific admin user's session, since (as noted
         * on Conversation's entity comment) ANY admin can pick up ANY
         * conversation — there's no single "assigned" admin. Every
         * currently-connected admin client subscribed to this topic
         * receives the new message notification simultaneously.
         */
        messagingTemplate.convertAndSend("/topic/admin-inbox", response);

        log.info("Customer {} sent message in conversation {}", customerId, conversation.getId());

        return response;

    }



    // sendAdminReply()

    public MessageResponse sendAdminReply(String conversationId, String content) {

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Conversation not found: " + conversationId));


        Message message = Message.builder()
                .conversationId(conversationId)
                .senderId(ADMIN_ID)
                .receiverId(conversation.getCustomerId())
                .message(content)
                .readStatus(false)
                .build();


        Message saved = messageRepository.save(message);

        conversation.setLastMessagePreview(truncate(content));
        conversation.setLastMessageAt(Instant.now());
        conversation.setUnreadByCustomer(conversation.getUnreadByCustomer() + 1);

        conversationRepository.save(conversation);


        MessageResponse response = toResponse(saved);


        // Push directly to THIS customer's session — recall the
        // customerId here is the same userId used as the WebSocket
        // session's Principal name, set in WebSocketAuthInterceptor
        messagingTemplate.convertAndSendToUser(
                conversation.getCustomerId(), "/queue/messages", response);


        log.info("Admin replied in conversation {}", conversationId);

        return response;
    }



    // getConversationHistory()

    public List<MessageResponse> getConversationHistory(String conversationId, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());

        return messageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId, pageable)
                .getContent()
                .stream()
                .map(this::toResponse)
                .toList();
        
    }
    
    
    
    // markAsReadByCustomer()

    public void markAsReadByCustomer(String customerId) {

        conversationRepository.findByCustomerId(customerId).ifPresent(conv -> {
            conv.setUnreadByCustomer(0);
            conversationRepository.save(conv);
        });
    }



    // markAsReadByAdmin()

    public void markAsReadByAdmin(String conversationId) {
        conversationRepository.findById(conversationId).ifPresent(conv -> {
            conv.setUnreadByAdmin(0);
            conversationRepository.save(conv);
        });
    }



    // closeConversation()  // Admin-controlled status changes

    public void closeConversation(String conversationId)
    {

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Conversation not found: " + conversationId));

        conversation.setStatus(Conversation.ConversationStatus.CLOSED);
        conversationRepository.save(conversation);

        log.info("Conversation {} closed by admin", conversationId);
    }



    // reopenConversation()

    public void reopenConversation(String conversationId) {

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Conversation not found: " + conversationId));

        conversation.setStatus(Conversation.ConversationStatus.OPEN);
        conversationRepository.save(conversation);

        log.info("Conversation {} reopened by admin", conversationId);
    }




    // PRIVATE HELPERS


    // createNewConversation()

    private Conversation createNewConversation(String customerId) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + customerId));

        Conversation conversation = Conversation.builder()
                .customerId(customerId)
                .customerName(customer.getFirstName() + " " + customer.getLastName())
                .status(Conversation.ConversationStatus.OPEN)
                .build();

        return conversationRepository.save(conversation);
    }


    private String truncate(String content) {
        return content.length() > 100 ? content.substring(0, 100) + "..." : content;
    }



    // toResponse()

    private MessageResponse toResponse(Message message) {

        return MessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .receiverId(message.getReceiverId())
                .message(message.getMessage())
                .readStatus(message.isReadStatus())
                .createdAt(message.getCreatedAt())
                .build();
    }
}


