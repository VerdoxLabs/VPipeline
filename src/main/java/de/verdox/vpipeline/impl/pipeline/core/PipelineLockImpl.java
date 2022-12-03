package de.verdox.vpipeline.impl.pipeline.core;

import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.pipeline.core.PipelineLock;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;
import java.util.function.Function;

public class PipelineLockImpl<T extends IPipelineData> implements PipelineLock<T> {
    private final PipelineImpl pipeline;
    private final Class<? extends T> type;
    private final UUID objectUUID;
    private final Lock readLock;
    private final Lock writeLock;

    public PipelineLockImpl(PipelineImpl pipeline, Class<? extends T> type, UUID objectUUID, Lock readLock, Lock writeLock) {
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
        if (!pipeline.isReady())
            NetworkLogger.getLogger().fine("Cancelled runOnReadLock operation because pipeline was shutted down.");
        else {
            try {
                readLock().lock();
                runnable.run();
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } finally {
                readLock.unlock();
            }
        }
        return this;
    }

    @Override
    @Nullable
    public <O> O getter(Function<? super T, ? extends O> getter) {
        if (!pipeline.isReady()) {
            NetworkLogger.getLogger().fine("Cancelled getter operation because pipeline was shutted down.");
            return null;
        } else {
            try {
                readLock().lock();
                var localCacheData = pipeline.getLocalCache().loadObject(getObjectType(), getObjectUUID());
                return getter.apply(localCacheData);
            } finally {
                readLock().unlock();
            }
        }
    }

    @Override
    public PipelineLock<T> runOnWriteLock(Runnable runnable) {
        if (!pipeline.isReady())
            NetworkLogger.getLogger().fine("Cancelled runOnWriteLock operation because pipeline was shutted down.");
        else {
            try {
                writeLock().lock();
                runnable.run();

            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } finally {
                writeLock.unlock();
            }
        }
        return this;
    }

    @Override
    public PipelineLock<T> performReadOperation(Consumer<T> reader) {
        if (!pipeline.isReady()) {
            NetworkLogger.getLogger().fine("Cancelled read operation because pipeline was shutted down.");
        } else {
            try {
                readLock().lock();
                NetworkLogger.getLogger().fine("Performing read operation");
                var localCacheData = pipeline.getLocalCache().loadObject(getObjectType(), getObjectUUID());
                if (localCacheData != null)
                    reader.accept(localCacheData);
            } finally {
                readLock.unlock();
            }
        }
        return this;
    }

    @Override
    public PipelineLock<T> performWriteOperation(Consumer<T> writer, boolean pushToNetwork) {
        var future = new CompletableFuture<PipelineLock<T>>();
        if (!pipeline.isReady()) {
            NetworkLogger.getLogger().fine("Cancelled write operation because pipeline was shutted down.");
            future.complete(this);
        } else {
            try {
                writeLock().lock();
                NetworkLogger.getLogger().fine("Performing write operation");
                var localCacheData = pipeline.getLocalCache().loadObject(getObjectType(), getObjectUUID());
                if (localCacheData != null) {
                    writer.accept(localCacheData);
                    if (pushToNetwork) {
                        localCacheData.save(true).thenRun(() -> future.complete(this));
                        future.join();
                    }
                }
            } finally {
                writeLock().unlock();
            }
        }
        return this;
    }
}
