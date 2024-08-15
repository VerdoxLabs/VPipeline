package de.verdox.vpipeline.api.pipeline.parts.cache.local;

import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import de.verdox.vpipeline.api.pipeline.parts.LocalCache;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.locks.Lock;

/**
 * Represents a thread safe data access to a {@link IPipelineData} object.
 * @param <T> The pipeline data encapsulated by this access
 */
public class DataAccess<T extends IPipelineData> {
    private final LocalCache localCache;
    private final Class<? extends T> type;
    private final UUID objectUUID;
    private final Lock readLock;
    private final Lock writeLock;

    DataAccess(LocalCache localCache, Class<? extends T> type, UUID objectUUID, Lock readLock, Lock writeLock) {
        this.localCache = localCache;
        this.type = type;
        this.objectUUID = objectUUID;
        this.readLock = readLock;
        this.writeLock = writeLock;
    }

    private boolean killed() {
        return !localCache.dataExist(type, objectUUID);
    }

    /**
     * Creates a read instruction for this data access object. The instruction acquires a {@link Lock}.
     * <p>
     * If the read lock is not available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until the
     * lock has been acquired
     * <p>
     * Several read locks can be acquired as long as there is no write lock taken on this object at the same time.
     * @return The read access
     * @throws AccessInvalidException thrown when the object was deleted from the network but the access is still used
     */
    public LockableAction.Read<T> read() throws AccessInvalidException {
        if (killed())
            throw new AccessInvalidException("DataAccess invalid for type " + type + " with uuid " + objectUUID);
        return new LockableAction.Read<>(readLock, localCache.loadObject(type, objectUUID));
    }

    /**
     * Creates a read instruction for this data access object. The instruction acquires a {@link Lock}.
     * <p>
     * If the lock is not available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until the
     * lock has been acquired
     * <p>
     * Only one write operation on the data may occur at a time in the network.
     * @return The read access
     * @throws AccessInvalidException thrown when the object was deleted from the network but the access is still used
     */
    public LockableAction.Write<T> write() throws AccessInvalidException {
        if (killed())
            throw new AccessInvalidException("DataAccess invalid for type " + type + " with uuid " + objectUUID);
        return new LockableAction.Write<>(writeLock, localCache.loadObject(type, objectUUID));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataAccess<?> that = (DataAccess<?>) o;
        return Objects.equals(localCache, that.localCache) && Objects.equals(type, that.type) && Objects.equals(objectUUID, that.objectUUID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(localCache, type, objectUUID);
    }
}
