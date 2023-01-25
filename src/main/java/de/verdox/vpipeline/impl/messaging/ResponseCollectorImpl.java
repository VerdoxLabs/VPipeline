package de.verdox.vpipeline.impl.messaging;

import de.verdox.vpipeline.api.messaging.instruction.ResponseCollector;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class ResponseCollectorImpl<T> implements ResponseCollector<T> {
    private final Map<UUID, CompletableFuture<T>> receivedValues = new ConcurrentHashMap<>();
    private final AtomicBoolean wasCancelled = new AtomicBoolean(false);
    private final Set<BiConsumer<? super T, ? super Throwable>> actions = ConcurrentHashMap.newKeySet();
    private final long amountReceivers;

    public ResponseCollectorImpl(long amountReceivers) {
        this.amountReceivers = amountReceivers;
    }

    void complete(UUID transmitter, T value) {
        if (wasCancelled.get())
            return;
        var future = new CompletableFuture<T>();
        actions.forEach(future::whenComplete);
        future.completeAsync(() -> value).orTimeout(10, TimeUnit.SECONDS);
        receivedValues.put(transmitter, future);
    }

    public ResponseCollectorImpl<T> whenResponseReceived(@NotNull BiConsumer<? super T, ? super Throwable> action) {
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
        return future.orTimeout(10, TimeUnit.SECONDS);
    }

    public long getAmountReceivedValues() {
        return receivedValues.size();
    }

    public boolean hasReceivedAllAnswers() {
        return getAmountReceivedValues() >= amountReceivers;
    }

    public void cancel() {
        wasCancelled.set(true);
        receivedValues.forEach((uuid, future) -> future.cancel(true));
    }
}