package com.rivern.chat.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

@Component
public class GrpcConfig {

    private Server server;
    private final RetrievalService retrievalService;

    public GrpcConfig(RetrievalService retrievalService) {
        this.retrievalService = retrievalService;
    }

    @PostConstruct
    public void start() throws Exception {
        server = ServerBuilder.forPort(9090)
                .addService(retrievalService)
                .build()
                .start();
        System.out.println("gRPC server started on port 9090");

    }

    @PreDestroy
    public void stop() {
        if (server != null) {
            server.shutdown();
            System.out.println("gRPC server shut down");
        }
    }

}
