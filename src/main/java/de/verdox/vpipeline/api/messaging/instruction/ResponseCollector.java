package de.verdox.vpipeline.api.messaging.instruction;

import de.verdox.vpipeline.impl.messaging.ResponseCollectorImpl;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public interface ResponseCollector<T> {
    ResponseCollectorImpl<T> whenResponseReceived(@NotNull BiConsumer<? super T, ? super Throwable> action);
    T waitForValue(Predicate<T> test);
    CompletableFuture<T> askForValue(Predicate<T> test);
    long getAmountReceivedValues();
    boolean hasReceivedAllAnswers();
}
