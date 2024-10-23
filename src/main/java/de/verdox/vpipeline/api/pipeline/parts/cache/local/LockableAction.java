package de.verdox.vpipeline.api.pipeline.parts.cache.local;

import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;

import java.io.Closeable;
import java.util.concurrent.locks.Lock;

public abstract class LockableAction<T extends IPipelineData> implements Closeable {
    protected final DataAccess<T> dataAccess;
    protected final Lock lock;
    protected T initValue;

    LockableAction(DataAccess<T> dataAccess, Lock lock, T initValue) {
        this.dataAccess = dataAccess;
        this.lock = lock;
        this.initValue = initValue;
        this.lock.lock();
    }

    @Override
    public void close() {
        this.lock.unlock();
    }

    //TODO: Wenn Daten remote gelöscht werden müssen die DataAccess Objekte davon irgendwie mitbekommen.
    public static class Read<T extends IPipelineData> extends LockableAction<T> {
        public Read(DataAccess<T> dataAccess, Lock lock, T initValue) {
            super(dataAccess, lock, initValue);
        }

        public T get() {
            return initValue;
        }
    }

    public static class Write<T extends IPipelineData> extends Read<T> {
        public Write(DataAccess<T> dataAccess, Lock lock, T initValue) {
            super(dataAccess, lock, initValue);
        }

        public void commitChanges(boolean saveToStorage) {
            initValue.getAttachedPipeline().getAttachedPipeline().getPipelineSynchronizer().sync(initValue, saveToStorage);
            dataAccess.notifySubscribers(get());
        }

        @Override
        public void close() {
            commitChanges(true);
            super.close();
        }
    }
}
