package de.verdox.vpipeline.impl.pipeline.parts;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.modules.AttachedPipeline;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import de.verdox.vpipeline.api.pipeline.datatypes.PipelineData;
import de.verdox.vpipeline.api.pipeline.parts.DataProviderLock;
import de.verdox.vpipeline.api.pipeline.parts.LocalCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class LocalCacheImpl implements LocalCache {
    private final Map<Class<? extends IPipelineData>, Map<UUID, IPipelineData>> cache = new ConcurrentHashMap<>();
    private final AttachedPipeline attachedPipeline;
    private final DataProviderLock dataProviderLock = new DataProviderLockImpl();

    public LocalCacheImpl() {
        this.attachedPipeline = new AttachedPipeline(GsonBuilder::create);
        NetworkLogger.info("Local Cache initialized");
    }


    @Override
    public JsonElement loadData(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID) {
        return dataProviderLock.executeOnWriteLock(() -> {
            IPipelineData data = loadObject(dataClass, objectUUID);
            if (data == null)
                return null;
            return data.serialize();
        });
    }

    @Override
    public synchronized <S extends IPipelineData> void saveObject(@NotNull S object) {
        Objects.requireNonNull(object, "object can't be null!");
        dataProviderLock.executeOnWriteLock(() -> {
/*        if (dataExist(object.getClass(), object.getObjectUUID())) {
            return;
        }*/
            cache.computeIfAbsent(object.getClass(), aClass -> new ConcurrentHashMap<>())
                 .put(object.getObjectUUID(), object);
            object.updateLastUsage();
            NetworkLogger
                    .fine("[LocalCache] Saved " + object + " [" + object.getObjectUUID() + "]");
            return null;
        });
    }

    @Override
    public synchronized boolean dataExist(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID) {
        Objects.requireNonNull(dataClass, "dataClass can't be null!");
        Objects.requireNonNull(objectUUID, "objectUUID can't be null!");
        return dataProviderLock.executeOnReadLock(() -> {
            if (!cache.containsKey(dataClass))
                return false;
            var foundData = cache.get(dataClass).getOrDefault(objectUUID, null);
            return foundData != null;
        });
    }

    @Override
    public synchronized void save(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID, @NotNull JsonElement dataToSave) {
        dataProviderLock.executeOnWriteLock(() -> {
            IPipelineData foundData = loadObject(dataClass, objectUUID);
            if (foundData == null)
                foundData = instantiateData(dataClass, objectUUID);
            foundData.updateLastUsage();
            foundData.deserialize(dataToSave);
            saveObject(foundData);
            NetworkLogger
                    .fine("[LocalCache] Updated " + foundData + " [" + foundData.getObjectUUID() + "]");
            return null;
        });
    }

    @Override
    public synchronized boolean remove(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID) {
        Objects.requireNonNull(dataClass, "dataClass can't be null!");
        Objects.requireNonNull(objectUUID, "objectUUID can't be null!");

        return dataProviderLock.executeOnWriteLock(() -> {
            if (!dataExist(dataClass, objectUUID))
                return false;
            IPipelineData data = cache.get(dataClass).remove(objectUUID);
            deleteFromCache(data);
            data.onDelete();
            NetworkLogger.fine("[LocalCache] Removed " + data + " [" + objectUUID + "]");
            return true;
        });
    }

    private void deleteFromCache(IPipelineData pipelineData) {
        cache.get(pipelineData.getClass()).remove(pipelineData.getObjectUUID());
        if (cache.get(pipelineData.getClass()).size() == 0)
            cache.remove(pipelineData.getClass());
    }

    @Override
    public synchronized Set<UUID> getSavedUUIDs(@NotNull Class<? extends IPipelineData> dataClass) {
        Objects.requireNonNull(dataClass, "dataClass can't be null!");
        return dataProviderLock.executeOnReadLock(() -> {
            if (!cache.containsKey(dataClass))
                return new HashSet<>();
            return cache.get(dataClass).keySet();
        });
    }

    @Override
    public AttachedPipeline getAttachedPipeline() {
        return attachedPipeline;
    }

    @Override
    public DataProviderLock getDataProviderLock() {
        return dataProviderLock;
    }

    @Nullable
    @Override
    public synchronized <S extends IPipelineData> S loadObject(@NotNull Class<? extends S> dataClass, @NotNull UUID objectUUID) {
        Objects.requireNonNull(dataClass, "dataClass can't be null!");
        Objects.requireNonNull(objectUUID, "objectUUID can't be null!");

        return dataProviderLock.executeOnReadLock(() -> {
            if (!dataExist(dataClass, objectUUID))
                return null;
            IPipelineData data = cache.get(dataClass).get(objectUUID);
            data.updateLastUsage();
            return dataClass.cast(data);
        });
    }

    @Override
    public synchronized <S extends IPipelineData> Set<S> loadAllData(@NotNull Class<? extends S> dataClass) {
        Objects.requireNonNull(dataClass, "dataClass can't be null!");
        return dataProviderLock.executeOnReadLock(() -> getSavedUUIDs(dataClass)
                .stream()
                .map(uuid -> loadObject(dataClass, uuid))
                .collect(Collectors.toSet()));
    }

    @Override
    public synchronized <S extends IPipelineData> S instantiateData(@NotNull Class<? extends S> dataClass, @NotNull UUID objectUUID) {
        Objects.requireNonNull(dataClass, "dataClass can't be null!");
        Objects.requireNonNull(objectUUID, "objectUUID can't be null!");

        return dataProviderLock.executeOnWriteLock(() -> {
            if (dataExist(dataClass, objectUUID))
                return loadObject(dataClass, objectUUID);

            NetworkLogger.fine("[LocalCache] Instantiated new data " + dataClass.getSimpleName() + " [" + objectUUID + "]");
            return PipelineData.instantiateData(attachedPipeline.getAttachedPipeline(), dataClass, objectUUID);
        });
    }

    @Override
    public void shutdown() {

    }
}
