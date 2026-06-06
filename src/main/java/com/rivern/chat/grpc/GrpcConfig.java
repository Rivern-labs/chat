package com.rivern.chat.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcConfig {

    @Bean(destroyMethod = "shutdownNow")
    public ManagedChannel retrievalChannel() {
        return ManagedChannelBuilder.forAddress("localhost", 9090)
                .usePlaintext()
                .build();
    }

    @Bean
    public RetrievalServiceGrpc.RetrievalServiceBlockingStub retrievalStub(ManagedChannel retrievalChannel) {
        return RetrievalServiceGrpc.newBlockingStub(retrievalChannel);
    }

}
