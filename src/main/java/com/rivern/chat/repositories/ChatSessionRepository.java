package com.rivern.chat.repositories;

import java.util.UUID;

import com.rivern.chat.entity.ChatSession;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatSessionRepository extends MongoRepository<ChatSession, UUID> {
    ChatSession findById(String sessionId);
}
