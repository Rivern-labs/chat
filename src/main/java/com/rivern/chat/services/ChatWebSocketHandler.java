package com.rivern.chat.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rivern.chat.dto.ChatRequest;
import com.rivern.chat.repositories.ChatSessionRepository;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper mapper;
    private final ChatService chatService;

    public ChatWebSocketHandler(ObjectMapper mapper, ChatService service,
            ChatSessionRepository chatSessionRepoistory) {
        this.mapper = mapper;
        this.chatService = service;

    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        ChatRequest request = mapper.readValue(message.getPayload(), ChatRequest.class);

        String sessionId = request.conversationId();
        String userMessage = request.message();
        chatService.getStreamingResponse(userMessage, sessionId).doOnNext(token -> {
            try {
                session.sendMessage(new TextMessage(token));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).subscribe();
    }

}
