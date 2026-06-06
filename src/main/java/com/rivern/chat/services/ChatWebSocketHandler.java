package com.rivern.chat.services;

import java.util.Arrays;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rivern.chat.dto.ChatRequest;
import com.rivern.chat.entity.ChatSession;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper mapper;
    private final ChatService chatService;

    public ChatWebSocketHandler(ObjectMapper mapper, ChatService service) {
        this.mapper = mapper;
        this.chatService = service;
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        ChatRequest request = mapper.readValue(message.getPayload(), ChatRequest.class);

        StringBuilder assistantReply = new StringBuilder();
        String query = session.getUri().getQuery();

        String sessionId = Arrays.stream(query.split("&")).filter(p -> p.startsWith("sessionId="))
                .map(p -> p.substring("sessionId=".length()))
                .findFirst()
                .orElseThrow();

        String userMessage = request.message();
        chatService.getStreamingResponse(userMessage, sessionId).doOnNext(token -> {
            assistantReply.append(token);
            try {
                synchronized (session) {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(token));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }).doOnError(error -> {
            try {
                if (session.isOpen())
                    session.sendMessage(new TextMessage("[ERROR]" + error.getMessage()));
            } catch (Exception e) {

            }
        }).doOnComplete(() -> chatService.appendAssistantMessage(request.conversationId(), assistantReply.toString()))
                .subscribe();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session)
            throws Exception {

        String query = session.getUri().getQuery();

        String sessionId = Arrays.stream(query.split("&")).filter(p -> p.startsWith("sessionId="))
                .map(p -> p.substring("sessionId=".length()))
                .findFirst()
                .orElseThrow();

        ChatSession chatSession = chatService.getChatSession(sessionId);
        if (chatSession.getMessages().size() == 2) {
            StringBuilder assistantReply = new StringBuilder();
            chatService.getInitialResponse(sessionId).doOnNext(token -> {
                assistantReply.append(token);
                try {
                    synchronized (session) {
                        if (session.isOpen()) {
                            session.sendMessage(new TextMessage(token));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }).doOnError(error -> {
                try {
                    if (session.isOpen())
                        session.sendMessage(new TextMessage("[ERROR]" + error.getMessage()));
                } catch (Exception e) {

                }
            }).doOnComplete(
                    () -> chatService.appendAssistantMessage(sessionId, assistantReply.toString()))
                    .subscribe();
        }
    }

}
