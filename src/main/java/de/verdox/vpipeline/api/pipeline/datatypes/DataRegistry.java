package de.verdox.vpipeline.api.pipeline.datatypes;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public interface DataRegistry {
    <S extends IPipelineData> void registerType(@NotNull String classifier, @NotNull Class<? extends S> type);

    Set<Class<? extends IPipelineData>> getAllTypes(@NotNull String... classifiers);

    boolean isTypeRegistered(Class<? extends IPipelineData> type);

    default <S extends IPipelineData> void registerType(@NotNull Class<? extends S> type) {
        registerType("", type);
    }

    @Nullable
    Class<? extends IPipelineData> getTypeByStorageId(@NotNull String storageIdentifier);
}
