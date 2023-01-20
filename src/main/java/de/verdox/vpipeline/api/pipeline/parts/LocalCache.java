package de.verdox.vpipeline.api.pipeline.parts;

import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 11:13
 */
public interface LocalCache extends DataProvider {
    //TODO: Update last use on LoadObject
    @Nullable
    <S extends IPipelineData> S loadObject(@NotNull Class<? extends S> dataClass, @NotNull UUID objectUUID);

    @NotNull
    default <S extends IPipelineData> S loadObjectOrThrow(@NotNull Class<? extends S> dataClass, @NotNull UUID objectUUID) {
        var data = loadObject(dataClass, objectUUID);
        if (data == null)
            throw new NoSuchElementException("Could not find an element of type " + dataClass.getSimpleName() + " with uuid " + objectUUID + " in local cache.");
        return data;
    }

    <S extends IPipelineData> void saveObject(@NotNull S object);

    <S extends IPipelineData> Set<S> loadAllData(@NotNull Class<? extends S> dataClass);

    <S extends IPipelineData> S instantiateData(@NotNull Class<? extends S> dataClass, @NotNull UUID objectUUID);
}
