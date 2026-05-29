package com.mysawit.pembayaran.grpc;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcClientTokenConfig {

    private static final Metadata.Key<String> INTERNAL_TOKEN_HEADER =
            Metadata.Key.of("x-internal-token", Metadata.ASCII_STRING_MARSHALLER);

    @Bean
    @GrpcGlobalClientInterceptor
    public ClientInterceptor internalTokenClientInterceptor(
            @Value("${mysawit.grpc.internal-token:}") String internalToken) {
        return new ClientInterceptor() {
            @Override
            public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                    MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
                return new ForwardingClientCall.SimpleForwardingClientCall<>(
                        next.newCall(method, callOptions)) {
                    @Override
                    public void start(Listener<RespT> responseListener, Metadata headers) {
                        if (internalToken != null && !internalToken.isBlank()) {
                            headers.put(INTERNAL_TOKEN_HEADER, internalToken);
                        }
                        super.start(responseListener, headers);
                    }
                };
            }
        };
    }
}
