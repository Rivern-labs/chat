package com.rivern.chat.repositories;

import java.util.Optional;

import com.rivern.chat.entity.ChatSession;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatSessionRepository extends MongoRepository<ChatSession, String> {
    Optional<ChatSession> findById(String sessionId);
}
