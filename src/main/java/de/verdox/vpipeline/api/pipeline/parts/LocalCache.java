package de.verdox.vpipeline.api.pipeline.parts;

import de.verdox.vpipeline.api.pipeline.parts.cache.local.DataAccess;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import de.verdox.vpipeline.api.pipeline.parts.cache.local.DataSubscriber;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

public interface LocalCache extends DataProvider {
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

    <S extends IPipelineData> DataAccess<S> createAccess(@NotNull Class<? extends S> dataClass, @NotNull UUID objectUUID);

    <T extends IPipelineData> void subscribe(@NotNull Class<? extends T> dataClass, @NotNull UUID objectUUID, DataSubscriber<T, ?> subscriber);

    <T extends IPipelineData> void removeSubscriber(DataSubscriber<T, ?> subscriber);

    <T extends IPipelineData> void notifySubscribers(T updatedObject);

    <T extends IPipelineData> boolean hasDataSubscribers(@NotNull Class<? extends T> dataClass, @NotNull UUID objectUUID);
}
