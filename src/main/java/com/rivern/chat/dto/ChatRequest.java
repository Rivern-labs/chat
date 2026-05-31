package com.rivern.chat.dto;

public record ChatRequest(
        String conversationId,
        String message) {
}
