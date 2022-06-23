package de.verdox.vpipeline.api.pipeline.datatypes;

import java.util.Set;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 13:15
 */
public interface IDataRegistry {
    <S extends IPipelineData> void registerType(String classifier, Class<? extends S> type);

    Set<Class<? extends IPipelineData>> getAllTypes(String... classifiers);

    default <S extends IPipelineData> void registerType(Class<? extends S> type) {
        registerType("", type);
    }
}
