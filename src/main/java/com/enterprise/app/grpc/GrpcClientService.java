package com.enterprise.app.grpc;

import com.google.common.util.concurrent.*;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class GrpcClientService {

    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceBlockingStub blockingStub;

    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceFutureStub futureStub;

    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceStub asyncStub;

    @GrpcClient("product-service")
    private ProductServiceGrpc.ProductServiceStub productStub;

    public UserResponse getUserById(String userId) {
        GetUserRequest request = GetUserRequest.newBuilder().setId(userId).build();
        return blockingStub.getUser(request);
    }

    public void getUserAsync(String userId) {
        ListenableFuture<UserResponse> future =
                futureStub.getUser(GetUserRequest.newBuilder().setId(userId).build());

        Futures.addCallback(future, new FutureCallback<>() {
            public void onSuccess(UserResponse result) {
                log.info("User: {}", result.getEmail());
            }
            public void onFailure(Throwable t) {
                log.error("Error", t);
            }
        }, MoreExecutors.directExecutor());
    }

    public void watchUserEvents(String userId) {
        asyncStub.watchUserEvents(
                GetUserRequest.newBuilder().setId(userId).build(),
                new StreamObserver<>() {
                    public void onNext(UserResponse value) {
                        log.info("Event: {}", value);
                    }
                    public void onError(Throwable t) {
                        log.error("Stream error", t);
                    }
                    public void onCompleted() {
                        log.info("Done");
                    }
                }
        );
    }

    public void uploadProducts(java.util.List<ProductRequest> products) {

        StreamObserver<ProductRequest> requestObserver =
                productStub.uploadProducts(new StreamObserver<>() {
                    public void onNext(BatchProductResponse value) {
                        log.info("Uploaded: {}", value.getCount());
                    }
                    public void onError(Throwable t) {
                        log.error("Upload error", t);
                    }
                    public void onCompleted() {
                        log.info("Upload done");
                    }
                });

        products.forEach(requestObserver::onNext);
        requestObserver.onCompleted();
    }
}