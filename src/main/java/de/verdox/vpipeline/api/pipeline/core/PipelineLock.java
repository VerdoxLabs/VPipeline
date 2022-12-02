package de.verdox.vpipeline.api.pipeline.core;

import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;

public interface PipelineLock<T extends IPipelineData> {
    Lock readLock();

    Lock writeLock();

    Class<? extends T> getObjectType();

    UUID getObjectUUID();

    PipelineLock<T> runOnReadLock(Runnable callable);
    PipelineLock<T> runOnWriteLock(Runnable callable);

    PipelineLock<T> performReadOperation(Consumer<T> reader);

    PipelineLock<T> performWriteOperation(Consumer<T> reader, boolean pushToNetwork);
}
