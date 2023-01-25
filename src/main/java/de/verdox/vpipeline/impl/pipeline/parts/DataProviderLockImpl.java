package de.verdox.vpipeline.impl.pipeline.parts;

import de.verdox.vpipeline.api.pipeline.parts.DataProviderLock;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

public class DataProviderLockImpl implements DataProviderLock {

    private final ReadWriteLock reentrantLock = new ReentrantReadWriteLock();

    @Override
    public <R> R executeOnReadLock(Supplier<R> function) {
/*        reentrantLock.readLock().lock();*/
        try {
            return function.get();
        } finally {
/*            reentrantLock.readLock().unlock();*/
        }
    }

    @Override
    public <R> R executeOnWriteLock(Supplier<R> function) {
/*        reentrantLock.writeLock().lock();*/
        try {
            return function.get();
        } finally {
 /*           reentrantLock.writeLock().unlock();*/
        }
    }
}
