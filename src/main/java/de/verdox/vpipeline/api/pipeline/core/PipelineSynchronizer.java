package de.verdox.vpipeline.api.pipeline.core;

import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface PipelineSynchronizer extends SystemPart {

    /**
     * Used to synchronize data between pipeline {@link DataSourceType}
     * @param source the data source type
     * @param destination the data destination type
     * @param dataClass the data class
     * @param objectUUID the object uuid
     */
    boolean synchronizePipelineData(@NotNull DataSourceType source, @NotNull DataSourceType destination, @NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID);

    /**
     * Used to synchronize the local data of an {@link IPipelineData} object to the pipeline
     * @param iPipelineData the data to sync
     * @param syncWithStorage whether to save the data to storage as well. If false the data is only pushed to the global cache.
     */
    default void sync(@NotNull IPipelineData iPipelineData, boolean syncWithStorage){
        sync(iPipelineData.getClass(), iPipelineData.getObjectUUID(), syncWithStorage);
    }

    /**
     * Used to synchronize the local data of a data object to the pipeline
     * @param dataClass the data type
     * @param objectUUID the data object uuid
     * @param syncWithStorage whether to save the data to storage as well. If false the data is only pushed to the global cache.
     */
    void sync(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID, boolean syncWithStorage);

    enum DataSourceType {
        LOCAL,
        GLOBAL_CACHE,
        GLOBAL_STORAGE;
    }
}
