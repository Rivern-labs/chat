package com.rivern.chat.services;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.rivern.chat.entity.ChatSession;
import com.rivern.chat.entity.ChatSession.ChatMessage;
import com.rivern.chat.grpc.RetrievalClient;
import com.rivern.chat.grpc.RetrieveContextResponse;
import com.rivern.chat.repositories.ChatSessionRepository;
import com.rivern.chat.utlis.SystemPrompts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;

@Service
public class ChatService {

    @Autowired
    private RetrievalClient retrievalClient;

    @Autowired
    private ChatClientService chatClientService;

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    public ChatSession getChatSession(String chatSessionId) {
        return chatSessionRepository.findById(chatSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid chat session ID"));
    }

    public Flux<String> getInitialResponse(String chatSessionId) {
        ChatSession chatSession = chatSessionRepository.findById(chatSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid chat session ID"));

        List<ChatSession.ChatMessage> updatedMessages = new ArrayList<>();
        if (chatSession.getMessages() != null) {
            updatedMessages.addAll(chatSession.getMessages());
        }

        List<Message> messages = buildTranscriptFromTurns(chatSession);
        return chatClientService.getStreamingResponse(messages);
    }

    public Flux<String> getStreamingResponse(String userMessage, String chatSessionId) {
        ChatSession chatSession = chatSessionRepository.findById(chatSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid chat session ID"));

        List<ChatSession.ChatMessage> updatedMessages = new ArrayList<>();
        if (chatSession.getMessages() != null) {
            updatedMessages.addAll(chatSession.getMessages());
        }

        updatedMessages.add(new ChatSession.ChatMessage(ChatSession.Sender.USER, userMessage));
        chatSession.setMessages(updatedMessages);
        chatSessionRepository.save(chatSession);

        List<Message> messages = buildTranscriptFromTurns(chatSession);
        return chatClientService.getStreamingResponse(messages);
    }

    public void appendAssistantMessage(String chatSessionId, String assistantMessage) {
        ChatSession chatSession = chatSessionRepository.findById(chatSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid chat session ID"));

        List<ChatSession.ChatMessage> updatedMessages = new ArrayList<>();
        if (chatSession.getMessages() != null) {
            updatedMessages.addAll(chatSession.getMessages());
        }

        updatedMessages.add(new ChatSession.ChatMessage(ChatSession.Sender.ASSISTANT, assistantMessage));
        chatSession.setMessages(updatedMessages);
        chatSessionRepository.save(chatSession);
    }

    public ChatSession createChatSession(String userId, String initialQuery) {
        RetrieveContextResponse response = retrievalClient.retrieveContext(initialQuery);
        logger.info("Retrieved context for query '{}'", initialQuery);
        logger.info(response.getAllFields().toString());
        logger.info("Retrieved prompt ready context: '{}'", response.getPromptReadyContext());

        String systemPrompt = SystemPrompts.getSessionSummaryPrompt(response.getPromptReadyContext());
        List<ChatSession.ChatMessage> messages = List.of(
                new ChatMessage(ChatSession.Sender.USER, initialQuery),
                new ChatMessage(ChatSession.Sender.SYSTEM, systemPrompt));

        String sessionId = UUID.randomUUID().toString();
        ChatSession chatSession = new ChatSession(sessionId, userId, messages, initialQuery);
        ChatSession session = chatSessionRepository.save(chatSession);

        return session;
    }

    public List<ChatSession> getUserChatSessions(String userId) {
        return chatSessionRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("No chat sessions found for user ID: " + userId));
    }

    private List<Message> buildTranscriptFromTurns(ChatSession chatSession) {
        List<ChatMessage> turns = chatSession.getMessages();
        if (turns == null || turns.isEmpty()) {
            return new ArrayList<>();
        }

        List<Message> transcript = new ArrayList<>();
        for (ChatMessage cm : turns) {
            if (cm == null || cm.getContent() == null || cm.getContent().isBlank()) {
                continue;
            }
            if (cm.getSender() == ChatSession.Sender.USER) {
                transcript.add(new UserMessage(cm.getContent()));
            } else if (cm.getSender() == ChatSession.Sender.ASSISTANT) {
                transcript.add(new AssistantMessage(cm.getContent()));
            } else if (cm.getSender() == ChatSession.Sender.SYSTEM) {
                transcript.add(new SystemMessage(cm.getContent()));
            }
        }
        return transcript;
    }

}
