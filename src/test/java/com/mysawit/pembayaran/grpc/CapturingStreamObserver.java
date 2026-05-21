package com.mysawit.pembayaran.grpc;

import io.grpc.stub.StreamObserver;

final class CapturingStreamObserver<T> implements StreamObserver<T> {

    T value;
    Throwable error;
    boolean completed;

    @Override
    public void onNext(T value) {
        this.value = value;
    }

    @Override
    public void onError(Throwable throwable) {
        this.error = throwable;
    }

    @Override
    public void onCompleted() {
        this.completed = true;
    }
}
