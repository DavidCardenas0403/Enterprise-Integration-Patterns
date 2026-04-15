package com.eip.ptp.grpc;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class OrderGrpcService extends OrderServiceGrpc.OrderServiceImplBase {

    @Override
    public void createOrder(OrderRequest request, StreamObserver<OrderResponse> responseObserver) {
        OrderResponse response = OrderResponse.newBuilder()
                .setOrderId(request.getOrderId())
                .setStatus("CREATED_VIA_GRPC")
                .setMessage("Order received via gRPC: " + request.getProduct())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}