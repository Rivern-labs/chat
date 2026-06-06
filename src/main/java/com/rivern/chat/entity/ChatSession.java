package com.rivern.chat.entity;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Document("chat_sessions")
public class ChatSession {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String caseId;

    private String caseTitle;

    private List<ChatMessage> messages;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessage {
        private Sender sender;
        private String content;
    }

    public enum Sender {
        USER,
        ASSISTANT,
        SYSTEM,
    };

    public ChatSession(String id, String userId, List<ChatMessage> messages) {
        this.id = id;
        this.userId = userId;
        this.messages = messages;
    }
}
