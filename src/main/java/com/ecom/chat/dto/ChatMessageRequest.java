package com.ecom.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;


@Data
public class ChatMessageRequest {

    @NotBlank(message = "Message cannot be empty")
    private String content;
}