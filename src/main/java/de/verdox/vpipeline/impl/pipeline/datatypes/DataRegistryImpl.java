package de.verdox.vpipeline.impl.pipeline.datatypes;

import de.verdox.vpipeline.api.pipeline.core.Pipeline;
import de.verdox.vpipeline.api.pipeline.datatypes.DataRegistry;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import de.verdox.vpipeline.api.pipeline.enums.PreloadStrategy;
import de.verdox.vpipeline.api.util.AnnotationResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 13:18
 */
public class DataRegistryImpl implements DataRegistry {
    private final Map<String, Set<Class<? extends IPipelineData>>> cache = new ConcurrentHashMap<>();
    private final Map<String, Class<? extends IPipelineData>> typesByDataStorageId = new ConcurrentHashMap<>();
    private final Pipeline pipeline;

    public DataRegistryImpl(Pipeline pipeline) {
        this.pipeline = pipeline;
    }

    @Override
    public <S extends IPipelineData> void registerType(@NotNull String classifier, @NotNull Class<? extends S> type) {
        Objects.requireNonNull(classifier);
        Objects.requireNonNull(type);
        var dataStorageID = AnnotationResolver.getDataStorageIdentifier(type);
        if (typesByDataStorageId.containsKey(dataStorageID))
            throw new IllegalArgumentException("Data storage identifier " + dataStorageID + " of type " + type.getSimpleName() + " is already registered for " + typesByDataStorageId
                    .get(dataStorageID)
                    .getSimpleName());

        var syncService = this.pipeline.getSynchronizingService();
        if (syncService != null && AnnotationResolver
                .getDataProperties(type)
                .preloadStrategy()
                .equals(PreloadStrategy.LOAD_BEFORE))
            this.pipeline.getSynchronizingService().getOrCreate(pipeline, type);

        var map = cache.computeIfAbsent(classifier, s -> new HashSet<>());
        typesByDataStorageId.put(dataStorageID, type);
        map.add(type);
    }

    @Override
    public Set<Class<? extends IPipelineData>> getAllTypes(@NotNull String... classifiers) {
        Objects.requireNonNull(classifiers);
        Set<Class<? extends IPipelineData>> set = new HashSet<>();
        if (classifiers.length == 0)
            cache.values().forEach(set::addAll);
        else
            Arrays.stream(classifiers)
                  .forEach(s -> set.addAll(cache.getOrDefault(s, new HashSet<>())));
        return set;
    }

    @Override
    @Nullable
    public Class<? extends IPipelineData> getTypeByStorageId(@NotNull String storageIdentifier) {
        Objects.requireNonNull(storageIdentifier);
        return typesByDataStorageId.get(storageIdentifier);
    }
}
