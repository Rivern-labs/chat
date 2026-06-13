package com.rivern.chat.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcConfig {

    @Value("${RETRIEVAL_SERVICE_GRPC_URL:retrieval-svc:9090}")
    private String retrievalAddress;

    @Bean(destroyMethod = "shutdownNow")
    public ManagedChannel retrievalChannel() {

        String[] parts = retrievalAddress.split(":");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);

        return ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
    }

    @Bean
    public RetrievalServiceGrpc.RetrievalServiceBlockingStub retrievalStub(
            ManagedChannel retrievalChannel) {
        return RetrievalServiceGrpc.newBlockingStub(retrievalChannel);
    }
}
