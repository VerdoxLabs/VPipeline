package de.verdox.vpipeline.api.pipeline.parts;

import java.util.function.Supplier;

public interface DataProviderLock {
    <R> R executeOnReadLock(Supplier<R> function);

    <R> R executeOnWriteLock(Supplier<R> function);
}
