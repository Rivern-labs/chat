package com.rivern.chat.config;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
        http.cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Everything else requires a valid JWT
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder)));

        return http.build();
    }

    @Bean
    public JWKSet authServiceJwkSet(@Value("auth.endpoint") String authServiceEndpoint) {
        if (authServiceEndpoint == null || authServiceEndpoint.isBlank()) {
            throw new IllegalStateException("AUTHSERVICE_ENDPOINT must be configured");
        }

        try {
            URI jwksUri = URI.create(ensureTrailingSlash(authServiceEndpoint)).resolve(".well-known/jwks.json");

            HttpRequest request = HttpRequest.newBuilder(jwksUri)
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException(
                        "Failed to fetch JWKS from " + jwksUri + " (HTTP " + response.statusCode() + ")");
            }

            return JWKSet.parse(response.body());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to fetch JWKS from AuthService", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while fetching JWKS from AuthService", e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse JWKS from AuthService", e);
        }
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSet authServiceJwkSet) {
        return token -> {
            try {
                SignedJWT signedJwt = SignedJWT.parse(token);
                JWTClaimsSet claimsSet = signedJwt.getJWTClaimsSet();

                RSAKey rsaKey = selectRsaKey(authServiceJwkSet, signedJwt.getHeader().getKeyID());
                JWSVerifier verifier = new RSASSAVerifier(rsaKey.toRSAPublicKey());

                if (!signedJwt.verify(verifier)) {
                    throw new JwtException("Invalid JWT signature");
                }

                validateTimestamps(claimsSet);

                return Jwt.withTokenValue(token)
                        .headers(headers -> headers.putAll(signedJwt.getHeader().toJSONObject()))
                        .claims(claims -> claims.putAll(claimsSet.getClaims()))
                        .build();
            } catch (JwtException e) {
                throw e;
            } catch (Exception e) {
                throw new JwtException("Failed to decode JWT", e);
            }
        };
    }

    private static String ensureTrailingSlash(String endpoint) {
        return endpoint.endsWith("/") ? endpoint : endpoint + "/";
    }

    private static RSAKey selectRsaKey(JWKSet jwkSet, String keyId) {
        List<JWK> keys = jwkSet.getKeys();

        if (keyId != null && !keyId.isBlank()) {
            for (JWK key : keys) {
                if (key instanceof RSAKey rsaKey && keyId.equals(rsaKey.getKeyID())) {
                    return rsaKey;
                }
            }
        }

        for (JWK key : keys) {
            if (key instanceof RSAKey rsaKey) {
                return rsaKey;
            }
        }

        throw new IllegalStateException("No RSA verification key found in JWKS");
    }

    private static void validateTimestamps(JWTClaimsSet claimsSet) {
        Instant now = Instant.now();

        Date expiresAt = claimsSet.getExpirationTime();
        if (expiresAt != null && now.isAfter(expiresAt.toInstant())) {
            throw new JwtException("JWT has expired");
        }

        Date notBefore = claimsSet.getNotBeforeTime();
        if (notBefore != null && now.isBefore(notBefore.toInstant())) {
            throw new JwtException("JWT is not yet valid");
        }
    }
}
