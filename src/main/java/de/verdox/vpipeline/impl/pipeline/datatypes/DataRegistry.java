package de.verdox.vpipeline.impl.pipeline.datatypes;

import de.verdox.vpipeline.api.pipeline.datatypes.IDataRegistry;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 13:18
 */
public class DataRegistry implements IDataRegistry {
    private final Map<String, Set<Class<? extends IPipelineData>>> cache = new ConcurrentHashMap<>();

    @Override
    public <S extends IPipelineData> void registerType(String classifier, Class<? extends S> type) {
        cache.computeIfAbsent(classifier, s -> new HashSet<>()).add(type);
    }

    @Override
    public Set<Class<? extends IPipelineData>> getAllTypes(String... classifiers) {
        Set<Class<? extends IPipelineData>> set = new HashSet<>();
        if (classifiers.length == 0)
            cache.values().forEach(set::addAll);
        else
            Arrays.stream(classifiers).filter(Objects::nonNull).forEach(s -> set.addAll(cache.getOrDefault(s, new HashSet<>())));
        return set;
    }
}
