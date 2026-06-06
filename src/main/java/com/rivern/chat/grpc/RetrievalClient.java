package com.rivern.chat.grpc;

import org.springframework.stereotype.Service;

@Service
public class RetrievalClient {

    private final RetrievalServiceGrpc.RetrievalServiceBlockingStub stub;

    public RetrievalClient(RetrievalServiceGrpc.RetrievalServiceBlockingStub stub) {
        this.stub = stub;
    }

    public RetrieveContextResponse retrieveContext(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("query must not be blank");
        }

        RetrieveContextRequest request = RetrieveContextRequest.newBuilder()
                .setQuery(query.trim())
                .build();

        return stub.retrieveContext(request);
    }
}
