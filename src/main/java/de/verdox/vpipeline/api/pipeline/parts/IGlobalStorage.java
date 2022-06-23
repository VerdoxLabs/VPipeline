package de.verdox.vpipeline.api.pipeline.parts;

import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import de.verdox.vpipeline.api.util.AnnotationResolver;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 11:14
 */
public interface IGlobalStorage extends IDataProvider {
    default String getSuffix(@NotNull Class<? extends IPipelineData> dataClass) {
        Objects.requireNonNull(dataClass, "dataClass can't be null!");
        return AnnotationResolver.getDataStorageIdentifier(dataClass);
    }

    default String getStoragePath(@NotNull Class<? extends IPipelineData> dataClass, @NotNull String suffix, @NotNull String separator) {
        Objects.requireNonNull(dataClass, "dataClass can't be null!");
        Objects.requireNonNull(suffix, "suffix can't be null!");
        return AnnotationResolver.getDataStorageClassifier(dataClass) + separator + suffix;
    }
}