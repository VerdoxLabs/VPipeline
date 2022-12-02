package de.verdox.vpipeline.impl.pipeline.core;

import de.verdox.vpipeline.api.pipeline.core.Pipeline;
import de.verdox.vpipeline.api.pipeline.core.PipelineLock;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;
import java.util.function.Function;

public class PipelineLockImpl<T extends IPipelineData> implements PipelineLock<T> {
    private final Pipeline pipeline;
    private final Class<? extends T> type;
    private final UUID objectUUID;
    private final Lock readLock;
    private final Lock writeLock;

    public PipelineLockImpl(Pipeline pipeline, Class<? extends T> type, UUID objectUUID, Lock readLock, Lock writeLock) {
        this.pipeline = pipeline;
        this.type = type;
        this.objectUUID = objectUUID;
        this.readLock = readLock;
        this.writeLock = writeLock;
    }

    @Override
    public Lock readLock() {
        return readLock;
    }

    @Override
    public Lock writeLock() {
        return writeLock;
    }

    @Override
    public Class<? extends T> getObjectType() {
        return type;
    }

    @Override
    public UUID getObjectUUID() {
        return objectUUID;
    }

    @Override
    public PipelineLock<T> runOnReadLock(Runnable runnable) {
        try {
            readLock().lock();
            runnable.run();
            return this;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            readLock.unlock();
        }
    }

    @Nullable
    public <O> O syncGetter(Function<? super T, ? extends O> getter) {
        try {
            readLock().lock();
            var localCacheData = pipeline.getLocalCache().loadObject(getObjectType(), getObjectUUID());
            return getter.apply(localCacheData);
        } finally {
            readLock().unlock();
        }
    }

    @Override
    public PipelineLock<T> runOnWriteLock(Runnable runnable) {
        try {
            writeLock().lock();
            runnable.run();
            return this;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public PipelineLock<T> performReadOperation(Consumer<T> reader) {
        try {
            readLock().lock();
            var localCacheData = pipeline.getLocalCache().loadObject(getObjectType(), getObjectUUID());
            if (localCacheData != null)
                reader.accept(localCacheData);
            return this;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public PipelineLock<T> performWriteOperation(Consumer<T> writer, boolean pushToNetwork) {
        try {
            readLock().lock();
            var localCacheData = pipeline.getLocalCache().loadObject(getObjectType(), getObjectUUID());
            if (localCacheData != null) {
                writer.accept(localCacheData);
                if (pushToNetwork)
                    localCacheData.save(true);
            }
            return this;
        } finally {
            readLock.unlock();
        }
    }
}
