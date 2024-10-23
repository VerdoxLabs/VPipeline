package de.verdox.vpipeline.api.pipeline.parts.cache.local;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.modules.AttachedPipeline;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import de.verdox.vpipeline.api.pipeline.datatypes.PipelineData;
import de.verdox.vpipeline.api.pipeline.parts.LocalCache;
import de.verdox.vpipeline.api.util.AnnotationResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class HashedLocalCache implements LocalCache {
    private final Map<Class<? extends IPipelineData>, Map<UUID, IPipelineData>> cache = new HashMap<>();
    private final Map<Class<? extends IPipelineData>, Map<UUID, DataAccess<IPipelineData>>> cachedAccess = new HashMap<>();
    private final Map<Class<? extends IPipelineData>, Map<UUID, Set<DataSubscriber<? extends IPipelineData, ?>>>> subscribers = new HashMap<>();
    private final AttachedPipeline attachedPipeline;
    private final ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();

    public HashedLocalCache() {
        this.attachedPipeline = new AttachedPipeline(GsonBuilder::create);
        NetworkLogger.info("Local Cache initialized");
    }

    @Override
    public JsonElement loadData(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID) {
        reentrantReadWriteLock.readLock().lock();
        try {
            IPipelineData data = loadObject(dataClass, objectUUID);
            if (data == null)
                return null;
            return data.serialize();
        } finally {
            reentrantReadWriteLock.readLock().unlock();
        }
    }

    @Override
    public <S extends IPipelineData> void saveObject(@NotNull S object) {
        Objects.requireNonNull(object, "object can't be null!");
        reentrantReadWriteLock.writeLock().lock();
        try {
            if (dataExist(object.getClass(), object.getObjectUUID()))
                remove(object.getClass(), object.getObjectUUID());
            cache.computeIfAbsent(object.getClass(), aClass -> new ConcurrentHashMap<>()).put(object.getObjectUUID(), object);
            notifySubscribers(object);
            object.updateLastUsage();
        } finally {
            reentrantReadWriteLock.writeLock().unlock();
        }
    }

    @Override
    public boolean dataExist(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID) {
        Objects.requireNonNull(dataClass, "dataClass can't be null!");
        Objects.requireNonNull(objectUUID, "objectUUID can't be null!");
        reentrantReadWriteLock.readLock().lock();
        try {
            if (!cache.containsKey(dataClass))
                return false;
            var foundData = cache.get(dataClass).getOrDefault(objectUUID, null);
            return foundData != null;
        } finally {
            reentrantReadWriteLock.readLock().unlock();
        }
    }

    @Override
    public void save(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID, @NotNull JsonElement dataToSave) {
        reentrantReadWriteLock.writeLock().lock();
        try {
            IPipelineData foundData = loadObject(dataClass, objectUUID);
            if (foundData == null)
                foundData = instantiateData(dataClass, objectUUID);
            foundData.updateLastUsage();
            foundData.deserialize(dataToSave);
            saveObject(foundData);
        } finally {
            reentrantReadWriteLock.writeLock().unlock();
        }
    }

    @Override
    public boolean remove(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID) {
        Objects.requireNonNull(dataClass, "dataClass can't be null!");
        Objects.requireNonNull(objectUUID, "objectUUID can't be null!");
        reentrantReadWriteLock.writeLock().lock();
        try {
            if (!dataExist(dataClass, objectUUID))
                return false;
            IPipelineData data = cache.get(dataClass).remove(objectUUID);
            deleteFromCache(data);
            data.onDelete();
            if (AnnotationResolver.getDataProperties(dataClass).debugMode())
                NetworkLogger.debug("[LocalCache] Removed " + data + " [" + objectUUID + "]");
            return true;
        } finally {
            reentrantReadWriteLock.writeLock().unlock();
        }
    }

    @Override
    public Set<UUID> getSavedUUIDs(@NotNull Class<? extends IPipelineData> dataClass) {
        Objects.requireNonNull(dataClass, "dataClass can't be null!");
        reentrantReadWriteLock.readLock().lock();
        try {
            if (!cache.containsKey(dataClass))
                return new HashSet<>();
            return cache.get(dataClass).keySet();
        } finally {
            reentrantReadWriteLock.readLock().unlock();
        }
    }

    @Nullable
    @Override
    public <S extends IPipelineData> S loadObject(@NotNull Class<? extends S> dataClass, @NotNull UUID objectUUID) {
        Objects.requireNonNull(dataClass, "dataClass can't be null!");
        Objects.requireNonNull(objectUUID, "objectUUID can't be null!");
        reentrantReadWriteLock.readLock().lock();
        try {
            if (!dataExist(dataClass, objectUUID))
                return null;
            IPipelineData data = cache.get(dataClass).get(objectUUID);
            data.updateLastUsage();
            return dataClass.cast(data);
        } finally {
            reentrantReadWriteLock.readLock().unlock();
        }
    }

    @Override
    public <S extends IPipelineData> Set<S> loadAllData(@NotNull Class<? extends S> dataClass) {
        Objects.requireNonNull(dataClass, "dataClass can't be null!");
        reentrantReadWriteLock.readLock().lock();
        try {
            return getSavedUUIDs(dataClass)
                    .stream()
                    .map(uuid -> loadObject(dataClass, uuid))
                    .collect(Collectors.toSet());
        } finally {
            reentrantReadWriteLock.readLock().unlock();
        }
    }

    @Override
    public <S extends IPipelineData> S instantiateData(@NotNull Class<? extends S> dataClass, @NotNull UUID objectUUID) {
        Objects.requireNonNull(dataClass, "dataClass can't be null!");
        Objects.requireNonNull(objectUUID, "objectUUID can't be null!");
        if (dataExist(dataClass, objectUUID))
            return loadObject(dataClass, objectUUID);

        if (AnnotationResolver.getDataProperties(dataClass).debugMode())
            NetworkLogger.debug("[LocalCache] Instantiated new data " + dataClass.getSimpleName() + " [" + objectUUID + "]");
        return PipelineData.instantiateData(attachedPipeline.getAttachedPipeline(), dataClass, objectUUID);
    }

    @Override
    public <S extends IPipelineData> DataAccess<S> createAccess(@NotNull Class<? extends S> dataClass, @NotNull UUID objectUUID) {
        if (!dataExist(dataClass, objectUUID))
            throw new IllegalArgumentException("No object in local cache with dataClass " + dataClass + " and uuid " + objectUUID);
        Lock objectReadLock = getAttachedPipeline().getAttachedPipeline().getNetworkDataLockingService().getReadLock(dataClass, objectUUID);
        Lock objectWriteLock = getAttachedPipeline().getAttachedPipeline().getNetworkDataLockingService().getWriteLock(dataClass, objectUUID);

        return (DataAccess<S>) cachedAccess.computeIfAbsent(dataClass, aClass -> new ConcurrentHashMap<>()).computeIfAbsent(objectUUID, uuid ->
                new DataAccess<>(this, dataClass, objectUUID, objectReadLock, objectWriteLock)
        );
    }

    @Override
    public <T extends IPipelineData> void subscribe(@NotNull Class<? extends T> dataClass, @NotNull UUID objectUUID, DataSubscriber<T, ?> subscriber) {
        subscriber.linkToLocalCache(dataClass, objectUUID);
        T data = loadObject(dataClass, objectUUID);
        if(data != null)
            subscriber.update(data);
        subscribers.computeIfAbsent(dataClass, aClass -> new ConcurrentHashMap<>())
                .computeIfAbsent(objectUUID, uuid -> new HashSet<>())
                .add(subscriber);
    }

    @Override
    public <T extends IPipelineData> void removeSubscriber(DataSubscriber<T, ?> subscriber) {
        if(subscriber.getDataClass() == null || subscriber.getObjectUUID() == null)
            return;
        if(!subscribers.containsKey(subscriber.getDataClass()) || !subscribers.get(subscriber.getDataClass()).containsKey(subscriber.getObjectUUID()))
            return;
        subscribers.get(subscriber.getDataClass()).get(subscriber.getObjectUUID()).remove(subscriber);
        subscriber.unlinkFromLocalCache();
    }

    @Override
    public <T extends IPipelineData> void notifySubscribers(T updatedObject) {
        if(!subscribers.containsKey(updatedObject.getClass()))
            return;
        if(!subscribers.get(updatedObject.getClass()).containsKey(updatedObject.getObjectUUID()))
            return;
        Set<DataSubscriber<? extends IPipelineData, ?>> subscriberSet = subscribers.get(updatedObject.getClass()).get(updatedObject.getObjectUUID());
        for (DataSubscriber<? extends IPipelineData, ?> dataSubscriber : subscriberSet)
            dataSubscriber.update(updatedObject);
    }

    @Override
    public <T extends IPipelineData> boolean hasDataSubscribers(@NotNull Class<? extends T> dataClass, @NotNull UUID objectUUID) {
        if(!subscribers.containsKey(dataClass) || !subscribers.get(dataClass).containsKey(objectUUID))
            return false;
        return !subscribers.get(dataClass).get(objectUUID).isEmpty();
    }

    @Override
    public AttachedPipeline getAttachedPipeline() {
        return attachedPipeline;
    }

    @Override
    public void shutdown() {

    }

    private void deleteFromCache(IPipelineData pipelineData) {
        cache.get(pipelineData.getClass()).remove(pipelineData.getObjectUUID());
        if (cache.get(pipelineData.getClass()).isEmpty())
            cache.remove(pipelineData.getClass());

        if (cachedAccess.containsKey(pipelineData.getClass())) {
            cachedAccess.get(pipelineData.getClass()).remove(pipelineData.getObjectUUID());
            if (cachedAccess.get(pipelineData.getClass()).isEmpty())
                cachedAccess.remove(pipelineData.getClass());
        }
    }
}
