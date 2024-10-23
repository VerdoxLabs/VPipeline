package de.verdox.vpipeline.api.pipeline.core;

import com.google.gson.GsonBuilder;
import de.verdox.vpipeline.api.pipeline.parts.cache.local.DataAccess;
import de.verdox.vpipeline.api.NetworkParticipant;
import de.verdox.vpipeline.api.pipeline.datatypes.DataRegistry;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import de.verdox.vpipeline.api.pipeline.datatypes.SynchronizingService;
import de.verdox.vpipeline.api.pipeline.parts.GlobalCache;
import de.verdox.vpipeline.api.pipeline.parts.GlobalStorage;
import de.verdox.vpipeline.api.pipeline.parts.LocalCache;
import de.verdox.vpipeline.api.pipeline.parts.cache.local.DataSubscriber;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public interface Pipeline extends SystemPart {
    /**
     * Returns the network participant of this Pipeline
     * @return the network participant
     */
    NetworkParticipant getNetworkParticipant();

    /**
     * Returns the {@link LocalCache}
     * @return the local cache
     */
    LocalCache getLocalCache();

    /**
     * Returns the {@link SynchronizingService}
     * @return the synchronizingService
     */
    @Nullable
    SynchronizingService getSynchronizingService();

    /**
     * Returns the {@link PipelineSynchronizer}
     * @return the pipelineSynchronizer
     */
    @NotNull
    PipelineSynchronizer getPipelineSynchronizer();

    /**
     * Returns the {@link GlobalCache}
     * @return the global cache
     */
    @Nullable
    GlobalCache getGlobalCache();

    /**
     * Checks if the pipeline is ready
     * @return true if the pipeline is ready
     */
    boolean isReady();

    /**
     * Returns the {@link GlobalStorage}
     * @return the global storage
     */
    @Nullable
    GlobalStorage getGlobalStorage();

    /**
     * Returns the {@link NetworkDataLockingService}
     * @return the networkDataLockingService
     */
    @NotNull
    NetworkDataLockingService getNetworkDataLockingService();

    /**
     * Returns the {@link DataRegistry}
     * @return the dataRegistry
     */
    @NotNull DataRegistry getDataRegistry();

    /**
     * Returns the {@link GsonBuilder}
     * @return the gson builder
     */
    @NotNull GsonBuilder getGsonBuilder();

    /**
     * Called on shutdown
     */
    void saveAll();

    /**
     * Preloads all data. Not used by the pipeline
     */
    void preloadAll();

    /**
     * Used to load {@link IPipelineData} into the {@link LocalCache} of the {@link Pipeline}.
     * When the {@link IPipelineData} was loaded successfully a {@link DataAccess} object is created that can be used
     * to perform read/write operations.
     * If the specified data is not available anywhere in the pipeline this method will return null.
     * @param type The data class
     * @param uuid the uuid of the data
     * @return the data access object if data was loaded successfully. Else it returns null
     * @param <T> the generic data type
     */
    @Nullable <T extends IPipelineData> DataAccess<T> load(@NotNull Class<? extends T> type, @NotNull UUID uuid);

    /**
     * Used to load {@link IPipelineData} into the {@link LocalCache} of the {@link Pipeline} or create the data if it was not found anywhere in the pipeline.
     * When the {@link IPipelineData} was loaded successfully a {@link DataAccess} object is created that can be used
     * to perform read/write operations.
     * @param type The data class
     * @param uuid the uuid of the data
     * @param immediateWriteOperation An immediate write operation that is run after the object creation
     * @return the data access object when data was loaded or created successfully.
     * @param <T> the generic data type
     */
    @NotNull <T extends IPipelineData> DataAccess<T> loadOrCreate(@NotNull Class<? extends T> type, @NotNull UUID uuid, @Nullable Consumer<T> immediateWriteOperation);

    /**
     * Used to load {@link IPipelineData} into the {@link LocalCache} of the {@link Pipeline} or create the data if it was not found anywhere in the pipeline.
     * When the {@link IPipelineData} was loaded successfully a {@link DataAccess} object is created that can be used
     * to perform read/write operations.
     * @param type The data class
     * @param uuid the uuid of the data
     * @return the data access object when data was loaded or created successfully.
     * @param <T> the generic data type
     */
    default @NotNull <T extends IPipelineData> DataAccess<T> loadOrCreate(@NotNull Class<? extends T> type, @NotNull UUID uuid){
        return loadOrCreate(type, uuid, null);
    }


    /**
     * Used to load all objects of {@link IPipelineData} into the {@link LocalCache} of the {@link Pipeline} or create the data if it was not found anywhere in the pipeline.
     * When the {@link IPipelineData} was loaded successfully a {@link DataAccess} object is created that can be used
     * to perform read/write operations.
     * @param type The data class
     * @return a set of data access objects when the data was loaded successfully.
     * @param <T> the generic data type
     */
    <T extends IPipelineData> Set<DataAccess<? extends T>> loadAllData(@NotNull Class<? extends T> type);

    /**
     * Used to check if a particular {@link IPipelineData} exists in the {@link Pipeline}.
     * @param type The data class
     * @param uuid the uuid of the data
     * @return true if the data exists anywhere in the pipeline.
     * @param <T> the generic data type
     */
    <T extends IPipelineData> boolean exist(@NotNull Class<? extends T> type, @NotNull UUID uuid);

    /**
     * Used to delete particular {@link IPipelineData} from the {@link Pipeline}.
     * @param type The data class
     * @param uuid the uuid of the data
     * @return true if the operation was successful.
     * @param <T> the generic data type
     */
    <T extends IPipelineData> boolean delete(@NotNull Class<? extends T> type, @NotNull UUID uuid);

    /**
     * Used to delete particular {@link IPipelineData} from the {@link LocalCache} after saving it to the {@link Pipeline}.
     * @param type The data class
     * @param uuid the uuid of the data
     * @return true if the operation was successful.
     * @param <T> the generic data type
     */
    <T extends IPipelineData> boolean saveAndRemoveFromLocalCache(@NotNull Class<? extends T> type, @NotNull UUID uuid);

    /**
     * Used to create a data subscriber for a particular {@link IPipelineData}.
     * @param type The data class
     * @param uuid the uuid of the data
     * @param subscriber the subscriber
     * @param <T> the generic data type
     */
    <T extends IPipelineData> void subscribe(@NotNull Class<? extends T> type, @NotNull UUID uuid, DataSubscriber<T, ?> subscriber);

    /**
     * Used to remove a data subscriber of a particular {@link IPipelineData}.
     * @param subscriber the subscriber
     * @param <T> the generic data type
     */
    <T extends IPipelineData> void removeSubscriber(DataSubscriber<T, ?> subscriber);
}
