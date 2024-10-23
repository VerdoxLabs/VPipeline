package de.verdox.vpipeline.api.messaging.instruction;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Waits and collects responses from network participants
 * @param <T>
 */
public interface ResponseCollector<T> {
    ResponseCollector<T> whenResponseReceived(@NotNull BiConsumer<? super T, ? super Throwable> action);
    T waitForValue(Predicate<T> test);
    CompletableFuture<T> askForValue(Predicate<T> test);
    long getAmountReceivedValues();
    boolean hasReceivedAllAnswers();
}
