package de.verdox.vpipeline.api.messaging.instruction.types;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class Response<T> {
    private final Map<UUID, CompletableFuture<T>> receivedValues = new ConcurrentHashMap<>();
    private final Set<BiConsumer<? super T, ? super Throwable>> actions = ConcurrentHashMap.newKeySet();

    void complete(UUID transmitter, T value) {
        var future = new CompletableFuture<T>();
        actions.forEach(future::whenComplete);
        future.completeAsync(() -> value);
        receivedValues.put(transmitter, future);
    }

    public Response<T> whenResponseReceived(@NotNull BiConsumer<? super T, ? super Throwable> action) {
        Objects.requireNonNull(action);
        actions.add(action);
        receivedValues.forEach((uuid, value) -> value.whenComplete(action));
        return this;
    }

    public T waitForValue(Predicate<T> test) {
        return askForValue(test).join();
    }

    public CompletableFuture<T> askForValue(Predicate<T> test) {
        var future = new CompletableFuture<T>();
        whenResponseReceived((t, throwable) -> {
            if (test.test(t))
                future.complete(t);
        });
        return future;
    }
}