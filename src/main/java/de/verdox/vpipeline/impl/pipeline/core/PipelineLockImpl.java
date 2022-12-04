package de.verdox.vpipeline.impl.pipeline.core;

import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.pipeline.core.PipelineLock;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;
import java.util.function.Function;

public class PipelineLockImpl<T extends IPipelineData> implements PipelineLock<T> {
    private final PipelineImpl pipeline;
    private final PipelineSynchronizerImpl pipelineSynchronizer;
    private final Class<? extends T> type;
    private final UUID objectUUID;
    private final Lock readLock;
    private final Lock writeLock;

    public PipelineLockImpl(PipelineImpl pipeline, PipelineSynchronizerImpl pipelineSynchronizer, Class<? extends T> type, UUID objectUUID, Lock readLock, Lock writeLock) {
        this.pipeline = pipeline;
        this.pipelineSynchronizer = pipelineSynchronizer;
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
    public synchronized void runOnReadLock(Runnable runnable) {
        if (!pipeline.isReady())
            NetworkLogger.info("Cancelled runOnReadLock operation because pipeline was shutted down.");
        else {
            try {
                readLock().lock();
                NetworkLogger.info("Acquired readLock on " + getObjectType().getSimpleName() + " with " + getObjectUUID());
                runnable.run();
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } finally {
                readLock.unlock();
                NetworkLogger.info("Released readLock on " + getObjectType().getSimpleName() + " with " + getObjectUUID());
            }
        }
    }

    @Override
    @Nullable
    public synchronized <O> O getter(Function<? super T, ? extends O> getter) {
        if (!pipeline.isReady()) {
            NetworkLogger.info("Cancelled getter operation because pipeline was shutted down.");
            return null;
        } else {
            try {
                readLock().lock();
                NetworkLogger.info("Acquired readLock on " + getObjectType().getSimpleName() + " with " + getObjectUUID());
                var localCacheData = pipeline.getLocalCache().loadObject(getObjectType(), getObjectUUID());
                NetworkLogger.info("Performing getter operation on " + localCacheData);
                return getter.apply(localCacheData);
            } finally {
                readLock().unlock();
                NetworkLogger.info("Released readLock on " + getObjectType().getSimpleName() + " with " + getObjectUUID());
            }
        }
    }

    @Override
    public synchronized void runOnWriteLock(Runnable runnable) {
        if (!pipeline.isReady())
            NetworkLogger.info("Cancelled runOnWriteLock operation because pipeline was shutted down.");
        else {
            try {
                writeLock().lock();
                NetworkLogger.info("Acquired writeLock on " + getObjectType().getSimpleName() + " with " + getObjectUUID());
                runnable.run();

            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } finally {
                writeLock.unlock();
                NetworkLogger.info("Released writeLock on " + getObjectType().getSimpleName() + " with " + getObjectUUID());
            }
        }
    }

    @Override
    public synchronized PipelineLock<T> performReadOperation(Consumer<T> reader) {
        if (!pipeline.isReady()) {
            NetworkLogger.info("Cancelled read operation because pipeline was shutted down.");
        } else {
            try {
                readLock().lock();
                NetworkLogger.info("Acquired readLock on " + getObjectType().getSimpleName() + " with " + getObjectUUID());
                var localCacheData = pipeline.getLocalCache().loadObject(getObjectType(), getObjectUUID());
                NetworkLogger.info("Performing read operation on " + localCacheData);
                if (localCacheData != null)
                    reader.accept(localCacheData);
            } finally {
                readLock.unlock();
                NetworkLogger.info("Released readLock on " + getObjectType().getSimpleName() + " with " + getObjectUUID());
            }
        }
        return this;
    }

    @Override
    public synchronized PipelineLock<T> performWriteOperation(Consumer<T> writer, boolean pushToNetwork) {
        if (!pipeline.isReady()) {
            NetworkLogger.info("Cancelled write operation because pipeline was shutted down.");
            return this;
        }
        try {
            writeLock().lock();
            NetworkLogger.info("Acquired writeLock on " + getObjectType().getSimpleName() + " with " + getObjectUUID());
            var localCacheData = pipeline.getLocalCache().loadObject(getObjectType(), getObjectUUID());
            NetworkLogger.info("Performing write operation on " + localCacheData);
            if (localCacheData != null) {
                writer.accept(localCacheData);
                if (pushToNetwork)
                    pipelineSynchronizer.doSync(localCacheData, pushToNetwork, () -> {
                    });
            }
        } finally {
            writeLock().unlock();
            NetworkLogger.info("Released writeLock on " + getObjectType().getSimpleName() + " with " + getObjectUUID());
        }
        return this;
    }

    @Override
    public synchronized PipelineLock<T> performSaveOperation(boolean saveToStorage) {
        if (!pipeline.isReady()) {
            NetworkLogger.info("Cancelled write operation because pipeline was shutted down.");
            return this;
        }

        try {
            writeLock().lock();
            NetworkLogger.info("Acquired writeLock on " + getObjectType().getSimpleName() + " with " + getObjectUUID());
            NetworkLogger.info("Performing save operation");
            var localCacheData = pipeline.getLocalCache().loadObject(getObjectType(), getObjectUUID());
            if (localCacheData != null)
                localCacheData.save(saveToStorage).join();
        } finally {
            writeLock().unlock();
            NetworkLogger.info("Released writeLock on " + getObjectType().getSimpleName() + " with " + getObjectUUID());
        }

        return this;
    }
}
