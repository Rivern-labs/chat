package com.rivern.chat.grpc;

import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

@Service
public class RetrievalService extends RetrievalServiceGrpc.RetrievalServiceImplBase {

    @Override
    public void retrieveContext(RetrieveContextRequest request,
            StreamObserver<RetrieveContextResponse> responseObserver) {
        String query = request == null ? "" : request.getQuery().trim();
        if (query.isEmpty()) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT.withDescription("query must not be blank").asRuntimeException());
            return;
        }

        String goGrpcTarget = System.getenv("RETRIEVAL_SERVICE_GRPC_URL");
        if (goGrpcTarget == null || goGrpcTarget.trim().isEmpty()) {
            responseObserver.onError(
                    Status.FAILED_PRECONDITION
                            .withDescription("RETRIEVAL_SERVICE_GRPC_URL is not configured")
                            .asRuntimeException());
            return;
        }

        ManagedChannel channel = null;
        try {
            channel = ManagedChannelBuilder.forTarget(goGrpcTarget.trim())
                    .usePlaintext()
                    .build();

            RetrievalServiceGrpc.RetrievalServiceBlockingStub stub = RetrievalServiceGrpc.newBlockingStub(channel);
            RetrieveContextResponse goResponse = stub.retrieveContext(
                    RetrieveContextRequest.newBuilder().setQuery(query).build());

            responseObserver.onNext(goResponse);
            responseObserver.onCompleted();
        } catch (StatusRuntimeException ex) {
            responseObserver.onError(ex);
        } catch (Exception ex) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Failed to retrieve legal context from Go backend")
                            .withCause(ex)
                            .asRuntimeException());
        } finally {
            if (channel != null) {
                channel.shutdownNow();
                try {
                    channel.awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
