package de.verdox.vpipeline.impl.pipeline.parts;

import com.google.gson.JsonElement;
import de.verdox.vpipeline.api.pipeline.core.IPipeline;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import de.verdox.vpipeline.api.pipeline.parts.ILocalCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 11:32
 */
public class LocalCache implements ILocalCache {
    private final Map<Class<? extends IPipelineData>, Map<UUID, IPipelineData>> cache = new ConcurrentHashMap<>();
    private IPipeline pipeline;

    public LocalCache() {

    }

    public void setPipeline(IPipeline pipeline){
        this.pipeline = pipeline;
    }



    @Override
    public JsonElement loadData(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID) {
        IPipelineData data = loadObject(dataClass, objectUUID);
        if (data == null)
            return null;
        return data.serialize();
    }

    @Override
    public boolean dataExist(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID) {
        Objects.requireNonNull(dataClass, "dataClass can't be null!");
        Objects.requireNonNull(objectUUID, "objectUUID can't be null!");
        if (!cache.containsKey(dataClass))
            return false;
        return cache.get(dataClass).containsKey(objectUUID);
    }

    @Override
    public void save(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID, @NotNull JsonElement dataToSave) {
        IPipelineData foundData = loadObject(dataClass, objectUUID);
        if (foundData == null)
            foundData = instantiateData(dataClass, objectUUID);
        foundData.updateLastUsage();
        foundData.deserialize(dataToSave);
        saveObject(foundData);
    }

    @Override
    public boolean remove(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID) {
        Objects.requireNonNull(dataClass, "dataClass can't be null!");
        Objects.requireNonNull(objectUUID, "objectUUID can't be null!");
        if (!dataExist(dataClass, objectUUID))
            return false;
        IPipelineData data = cache.get(dataClass).remove(objectUUID);
        data.markRemoval();
        if (cache.get(dataClass).size() == 0)
            cache.remove(dataClass);
        return true;
    }

    @Override
    public Set<UUID> getSavedUUIDs(@NotNull Class<? extends IPipelineData> dataClass) {
        Objects.requireNonNull(dataClass, "dataClass can't be null!");
        if (!cache.containsKey(dataClass))
            return new HashSet<>();
        return cache.get(dataClass).keySet();
    }

    @Nullable
    @Override
    public <S extends IPipelineData> S loadObject(@NotNull Class<? extends S> dataClass, @NotNull UUID objectUUID) {
        Objects.requireNonNull(dataClass, "dataClass can't be null!");
        Objects.requireNonNull(objectUUID, "objectUUID can't be null!");
        if (!dataExist(dataClass, objectUUID))
            return null;
        IPipelineData data = cache.get(dataClass).get(objectUUID);
        data.updateLastUsage();
        return dataClass.cast(data);
    }

    @Override
    public <S extends IPipelineData> void saveObject(@NotNull S object) {
        Objects.requireNonNull(object, "object can't be null!");
        if (dataExist(object.getClass(), object.getObjectUUID()))
            return;
        cache.computeIfAbsent(object.getClass(), aClass -> new ConcurrentHashMap<>());
        cache.get(object.getClass()).put(object.getObjectUUID(), object);
        object.updateLastUsage();
    }

    @Override
    public <S extends IPipelineData> Set<S> loadAllData(@NotNull Class<? extends S> dataClass) {
        Objects.requireNonNull(dataClass, "dataClass can't be null!");
        return getSavedUUIDs(dataClass).stream().map(uuid -> loadObject(dataClass, uuid)).collect(Collectors.toSet());
    }

    @Override
    public <S extends IPipelineData> S instantiateData(@NotNull Class<? extends S> dataClass, @NotNull UUID objectUUID) {
        Objects.requireNonNull(dataClass, "dataClass can't be null!");
        Objects.requireNonNull(objectUUID, "objectUUID can't be null!");

        if (dataExist(dataClass, objectUUID))
            return loadObject(dataClass, objectUUID);

        try {
            S dataObject = dataClass.getDeclaredConstructor(IPipeline.class, UUID.class).newInstance(pipeline, objectUUID);
            dataObject.updateLastUsage();
            return dataClass.cast(dataObject);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
            return null;
        }
    }
}
