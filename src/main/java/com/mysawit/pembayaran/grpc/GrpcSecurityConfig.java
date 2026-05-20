package com.mysawit.pembayaran.grpc;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcSecurityConfig {

    private static final Metadata.Key<String> INTERNAL_TOKEN_HEADER =
            Metadata.Key.of("x-internal-token", Metadata.ASCII_STRING_MARSHALLER);

    @Bean
    @GrpcGlobalServerInterceptor
    public ServerInterceptor internalTokenInterceptor(
            @Value("${mysawit.grpc.internal-token:}") String internalToken) {
        return new ServerInterceptor() {
            @Override
            public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                    ServerCall<ReqT, RespT> call,
                    Metadata headers,
                    ServerCallHandler<ReqT, RespT> next) {
                if (!hasText(internalToken) || internalToken.equals(headers.get(INTERNAL_TOKEN_HEADER))) {
                    return next.startCall(call, headers);
                }

                call.close(
                        Status.UNAUTHENTICATED.withDescription("Invalid gRPC internal token"),
                        new Metadata());
                return new ServerCall.Listener<>() {
                };
            }
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
