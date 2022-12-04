package de.verdox.vpipeline.api.pipeline.core;

import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;

import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;
import java.util.function.Function;

public interface PipelineLock<T extends IPipelineData> {
    Lock readLock();

    Lock writeLock();

    <O> O getter(Function<? super T, ? extends O> getter);

    Class<? extends T> getObjectType();

    UUID getObjectUUID();

    void runOnReadLock(Runnable callable);

    void runOnWriteLock(Runnable callable);

    //TODO: Implement callback function
    PipelineLock<T> performReadOperation(Consumer<T> reader);

    PipelineLock<T> performWriteOperation(Consumer<T> writer, boolean pushToNetwork);
    PipelineLock<T> performSaveOperation(boolean saveToStorage);

    default PipelineLock<T> performWriteOperation(Consumer<T> writer) {
        return performWriteOperation(writer, true);
    }
}
