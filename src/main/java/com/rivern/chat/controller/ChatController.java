package com.rivern.chat.controller;

import java.util.List;
import java.util.Map;

import com.rivern.chat.dto.NewChatSessionRequest;
import com.rivern.chat.dto.UsersChatResponse;
import com.rivern.chat.entity.ChatSession;
import com.rivern.chat.services.ChatService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/new")
    public ResponseEntity<String> createChatSession(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody NewChatSessionRequest request) {

        String userId = jwt.getSubject();

        ChatSession session = chatService.createChatSession(userId, request.initialQuery());

        return ResponseEntity.ok(session.getId().toString());
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<UsersChatResponse>> getUsersChatSessions(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        List<ChatSession> sessions = chatService.getUserChatSessions(userId);

        List<UsersChatResponse> response = sessions.stream()
                .map(session -> new UsersChatResponse(session.getId(), session.getInitialQuery()))
                .toList();
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred";

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", message));
    }

}
