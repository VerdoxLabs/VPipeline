package de.verdox.vpipeline.api.pipeline.parts.cache.local;

import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;

import java.io.Closeable;
import java.util.concurrent.locks.Lock;

public abstract class LockableAction<T extends IPipelineData> implements Closeable {
    protected final Lock lock;
    protected T initValue;

    public LockableAction(Lock lock, T initValue) {
        this.lock = lock;
        this.initValue = initValue;
        this.lock.lock();
    }

    @Override
    public final void close() {
        this.lock.unlock();
    }

    //TODO: Wenn Daten remote gelöscht werden müssen die DataAccess Objekte davon irgendwie mitbekommen.
    public static class Read<T extends IPipelineData> extends LockableAction<T> {
        public Read(Lock lock, T initValue) {
            super(lock, initValue);
        }

        public T get() {
            return initValue;
        }
    }

    public static class Write<T extends IPipelineData> extends Read<T> {
        public Write(Lock lock, T initValue) {
            super(lock, initValue);
        }

        public void commitChanges(boolean saveToStorage) {
            initValue.getAttachedPipeline().getAttachedPipeline().getPipelineSynchronizer().sync(initValue, saveToStorage);
        }
    }
}
