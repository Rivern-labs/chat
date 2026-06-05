package com.rivern.chat.controller;

import com.rivern.chat.dto.ChatRequest;
import com.rivern.chat.dto.NewChatSessionRequest;
import com.rivern.chat.entity.ChatSession;
import com.rivern.chat.grpc.RetrievalServiceGrpc;
import com.rivern.chat.grpc.RetrieveContextRequest;
import com.rivern.chat.grpc.RetrieveContextResponse;
import com.rivern.chat.services.ChatService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private RetrievalServiceGrpc.RetrievalServiceBlockingStub retrievalStub;

    @RequestMapping("/new")
    public ResponseEntity<String> createChatSession(Authentication authentication,
            @RequestBody NewChatSessionRequest request) {

        try {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            String userId = jwt.getSubject();

            RetrieveContextRequest contextRequest = RetrieveContextRequest.newBuilder()
                    .setQuery(request.initialQuery())
                    .build();

            RetrieveContextResponse response = retrievalStub.retrieveContext(contextRequest);

            ChatSession session = chatService.createChatSession(userId, request.initialQuery(),
                    response.getPromptReadyContext());

            return ResponseEntity.ok(session.getId().toString());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to create chat session: " + e.getMessage());
        }

    }

}
