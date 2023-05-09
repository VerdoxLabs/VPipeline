package de.verdox.vpipeline.impl.pipeline.core;

import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.pipeline.core.PipelineLock;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import de.verdox.vpipeline.api.pipeline.datatypes.customtypes.DataReference;
import de.verdox.vpipeline.api.util.AnnotationResolver;
import org.jetbrains.annotations.Nullable;
import org.redisson.api.RLock;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;
import java.util.function.Function;

//TODO: Bulk Writes / Reads on whole Collection.
public class PipelineLockImpl<T extends IPipelineData> implements PipelineLock<T> {
    private final PipelineImpl pipeline;
    private final PipelineSynchronizerImpl pipelineSynchronizer;
    private final Class<? extends T> type;
    private final UUID objectUUID;
    private final Lock readLock;
    private final Lock writeLock;

    public PipelineLockImpl(PipelineImpl pipeline, PipelineSynchronizerImpl pipelineSynchronizer, Class<? extends T> type, UUID objectUUID, Lock readLock, Lock writeLock) {
        //TODO: Exclusive write rights for data that is only updated on one server client at a time.
        // Damit kann ein Inventar erst freigegeben werden für write lock wenn der exclusive lock entfernt wurde. Ansonsten müssen die anderen clients halt drauf warten, dass sie ihn bekommen.
        // Readen geht ganz normal. Es ist also nur ein Lock der verhindert, dass andere den write lock acquiren können
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
            NetworkLogger.warning("Cancelled runOnReadLock operation because pipeline was shutted down.");
        else {
            try {
                lockRead();
                if (AnnotationResolver.getDataProperties(type).debugMode())
                    NetworkLogger.debug("Acquired readLock on " + getObjectType().getSimpleName() + " with " + getObjectUUID()+ " [" + System.currentTimeMillis() + "]");
                runnable.run();
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } finally {
                readLock.unlock();
                if (AnnotationResolver.getDataProperties(type).debugMode())
                    NetworkLogger.debug("Released readLock on " + getObjectType().getSimpleName() + " with " + getObjectUUID()+ " [" + System.currentTimeMillis() + "]");
            }
        }
    }

    @Override
    @Nullable
    public synchronized <O> O getter(Function<? super T, ? extends O> getter) {
        if (!pipeline.isReady()) {
            NetworkLogger.warning("Cancelled getter operation because pipeline was shutted down.");
            return null;
        } else {
            try {
                lockRead();
                if (AnnotationResolver.getDataProperties(type).debugMode())
                    NetworkLogger.debug("Acquired readLock on " + getObjectType().getSimpleName() + " with " + getObjectUUID()+ " [" + System.currentTimeMillis() + "]");
                var localCacheData = pipeline.getLocalCache().loadObject(getObjectType(), getObjectUUID());
                return getter.apply(localCacheData);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } finally {
                readLock().unlock();
                if (AnnotationResolver.getDataProperties(type).debugMode())
                    NetworkLogger.debug("Released readLock on " + getObjectType().getSimpleName() + " with " + getObjectUUID()+ " [" + System.currentTimeMillis() + "]");
            }
        }
    }

    @Override
    public synchronized void runOnWriteLock(Runnable runnable) {
        if (!pipeline.isReady())
            NetworkLogger.warning("Cancelled runOnWriteLock operation because pipeline was shutted down.");
        else {
            try {
                lockWrite();
                if (AnnotationResolver.getDataProperties(type).debugMode())
                    NetworkLogger.debug("Acquired writeLock on " + getObjectType().getSimpleName() + " with " + getObjectUUID()+ " [" + System.currentTimeMillis() + "]");
                runnable.run();
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } finally {
                writeLock.unlock();
                if (AnnotationResolver.getDataProperties(type).debugMode())
                    NetworkLogger.debug("Released writeLock on " + getObjectType().getSimpleName() + " with " + getObjectUUID()+ " [" + System.currentTimeMillis() + "]");
            }
        }
    }

    @Override
    public synchronized PipelineLock<T> performReadOperation(Consumer<T> reader) {
        if (!pipeline.isReady()) {
            NetworkLogger.warning("Cancelled read operation because pipeline was shutted down.");
        } else {
            try {
                lockRead();
                if (AnnotationResolver.getDataProperties(type).debugMode())
                    NetworkLogger.debug("Acquired readLock on " + getObjectType().getSimpleName() + " with " + getObjectUUID()+ " [" + System.currentTimeMillis() + "]");
                var localCacheData = pipeline.getLocalCache().loadObject(getObjectType(), getObjectUUID());
                if (localCacheData != null)
                    reader.accept(localCacheData);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } finally {
                readLock.unlock();
                if (AnnotationResolver.getDataProperties(type).debugMode())
                    NetworkLogger.debug("Released readLock on " + getObjectType().getSimpleName() + " with " + getObjectUUID()+ " [" + System.currentTimeMillis() + "]");
            }
        }
        return this;
    }

    @Override
    public synchronized PipelineLock<T> performWriteOperation(Consumer<T> writer, boolean pushToNetwork) {
        if (!pipeline.isReady()) {
            NetworkLogger.warning("Cancelled write operation because pipeline was shutted down.");
            return this;
        }
        try {
            lockWrite();
            if (AnnotationResolver.getDataProperties(type).debugMode())
                NetworkLogger.debug("Acquired writeLock on " + getObjectType().getSimpleName() + " with " + getObjectUUID() + " [" + System.currentTimeMillis() + "]");
            var localCacheData = pipeline.getLocalCache().loadObject(getObjectType(), getObjectUUID());
            if (localCacheData != null) {
                writer.accept(localCacheData);
                if (pushToNetwork) {
                    pipelineSynchronizer.doSync(localCacheData, true, () -> {
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            writeLock().unlock();
            if (AnnotationResolver.getDataProperties(type).debugMode())
                NetworkLogger.debug("Released writeLock on " + getObjectType().getSimpleName() + " with " + getObjectUUID() + " [" + System.currentTimeMillis() + "]");
        }
        return this;
    }

    @Override
    public synchronized PipelineLock<T> performSaveOperation(boolean saveToStorage) {
        if (!pipeline.isReady()) {
            NetworkLogger.warning("Cancelled write operation because pipeline was shutted down.");
            return this;
        }

        try {
            lockWrite();
            if (AnnotationResolver.getDataProperties(type).debugMode())
                NetworkLogger.debug("Acquired writeLock on " + getObjectType().getSimpleName() + " with " + getObjectUUID()+ " [" + System.currentTimeMillis() + "]");
            var localCacheData = pipeline.getLocalCache().loadObject(getObjectType(), getObjectUUID());
            if (localCacheData != null)
                localCacheData.save(saveToStorage).orTimeout(5, TimeUnit.SECONDS).join();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            writeLock().unlock();
            if (AnnotationResolver.getDataProperties(type).debugMode())
                NetworkLogger.debug("Released writeLock on " + getObjectType().getSimpleName() + " with " + getObjectUUID()+ " [" + System.currentTimeMillis() + "]");
        }

        return this;
    }

    @Override
    public DataReference<T> asReference() {
        return pipeline.createDataReference(getObjectType(), getObjectUUID());
    }

    private void lockRead() {
        if (readLock instanceof RLock rLock)
            rLock.lock(10, TimeUnit.SECONDS);
        else
            readLock.lock();
    }

    private void lockWrite() {
        if (writeLock instanceof RLock rLock)
            rLock.lock(10, TimeUnit.SECONDS);
        else
            writeLock.lock();
    }
}
