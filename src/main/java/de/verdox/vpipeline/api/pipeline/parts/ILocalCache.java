package de.verdox.vpipeline.api.pipeline.parts;

import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.UUID;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 11:13
 */
public interface ILocalCache extends IDataProvider {
    //TODO: Update last use on LoadObject
    @Nullable
    <S extends IPipelineData> S loadObject(@NotNull Class<? extends S> dataClass, @NotNull UUID objectUUID);
    <S extends IPipelineData> void saveObject(@NotNull S object);
    <S extends IPipelineData> Set<S> loadAllData(@NotNull Class<? extends S> dataClass);
    <S extends IPipelineData> S instantiateData(@NotNull Class<? extends S> dataClass, @NotNull UUID objectUUID);
}
