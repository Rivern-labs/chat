package com.rivern.chat.config;

import java.util.Arrays;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtDecoder jwtDecoder;

    private static final Logger logger = LoggerFactory.getLogger(JwtHandshakeInterceptor.class);

    public JwtHandshakeInterceptor(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
            Map<String, Object> attributes) throws Exception {
        String query = request.getURI().getQuery();
        if (query == null || query.isBlank()) {
            logger.warn("No query parameters found in the request URI.");
            return false;
        }

        String token = Arrays.stream(query.split("&"))
                .filter(p -> p.startsWith("token="))
                .map(p -> p.substring("token=".length()))
                .findFirst()
                .orElse(null);

        logger.info("Extracted token: {}", token);

        if (token == null) {
            return false;
        }

        try {
            Jwt jwt = jwtDecoder.decode(token);
            attributes.put("userId", jwt.getSubject());
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        logger.info("WebSocket handshake authorized for userId: {}", attributes.get("userId"));

        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
            @Nullable Exception exception) {

    }
}
