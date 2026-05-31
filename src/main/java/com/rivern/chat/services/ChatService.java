package com.rivern.chat.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.rivern.chat.entity.ChatSession;
import com.rivern.chat.entity.ChatSession.ChatMessage;
import com.rivern.chat.repositories.ChatSessionRepoistory;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;

@Service
public class ChatService {

    @Autowired
    private ChatClientService chatClientService;

    @Autowired
    private ChatSessionRepoistory chatSessionRepoistory;

    public Flux<String> getStreamingResponse(String userMessage, String chatSessionId) {
        Optional<ChatSession> session = chatSessionRepoistory.findById(UUID.fromString(chatSessionId));
        if (session.isEmpty()) {
            throw new IllegalArgumentException("Invalid chat session ID");
        }

        ChatSession chatSession = session.get();

        List<Message> messages = buildTranscriptFromTurns(chatSession);

        return chatClientService.getStreamingResponse(messages);

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
            }
        }
        return transcript;
    }

}
