package de.verdox.vpipeline.api.pipeline.core;

import com.google.gson.GsonBuilder;
import de.verdox.vpipeline.api.NetworkParticipant;
import de.verdox.vpipeline.api.pipeline.datatypes.DataRegistry;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import de.verdox.vpipeline.api.pipeline.datatypes.SynchronizingService;
import de.verdox.vpipeline.api.pipeline.datatypes.customtypes.DataReference;
import de.verdox.vpipeline.api.pipeline.parts.GlobalCache;
import de.verdox.vpipeline.api.pipeline.parts.GlobalStorage;
import de.verdox.vpipeline.api.pipeline.parts.LocalCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface Pipeline extends SystemPart {
    NetworkParticipant getNetworkParticipant();

    LocalCache getLocalCache();

    @Nullable
    SynchronizingService getSynchronizingService();

    @NotNull
    PipelineSynchronizer getPipelineSynchronizer();

    @Nullable
    GlobalCache getGlobalCache();

    boolean isReady();

    @Nullable
    GlobalStorage getGlobalStorage();

    @NotNull DataRegistry getDataRegistry();

    @NotNull GsonBuilder getGsonBuilder();

    /**
     * Called on shutdown
     */
    void saveAll();

    /**
     * The pipeline itself does not call this method. But when an implementation uses this API, the developer can call this function whenever it is a good idea implementation-wise.
     */
    void preloadAll();

    /**
     * @param loadCallback This callback is executed immediately after the pipeline task was performed. The lock on the object that is needed to perform this task is released before completing the future object. If you want to perform a task within the lock that is needed to perform this task then the callback is what you want to use.
     * @return A Pipeline lock only if there is a data somewhere in the pipeline
     */
    @Nullable <T extends IPipelineData> CompletableFuture<PipelineLock<T>> load(@NotNull Class<? extends T> type, @NotNull UUID uuid, @Nullable Consumer<T> loadCallback);

    default @Nullable <T extends IPipelineData> CompletableFuture<PipelineLock<T>> load(@NotNull Class<? extends T> type, @NotNull UUID uuid) {
        return load(type, uuid, null);
    }

    /**
     * @param loadCallback This callback is executed immediately after the pipeline task was performed. The lock on the object that is needed to perform this task is released before completing the future object. If you want to perform a task within the lock that is needed to perform this task then the callback is what you want to use.
     * @return A Pipeline lock only in any case
     */
    @NotNull <T extends IPipelineData> CompletableFuture<PipelineLock<T>> loadOrCreate(@NotNull Class<? extends T> type, @NotNull UUID uuid, @Nullable Consumer<T> loadCallback);

    default @NotNull <T extends IPipelineData> CompletableFuture<PipelineLock<T>> loadOrCreate(@NotNull Class<? extends T> type, @NotNull UUID uuid) {
        return loadOrCreate(type, uuid, null);
    }

    @NotNull <T extends IPipelineData> CompletableFuture<Set<DataReference<T>>> loadAllData(@NotNull Class<? extends T> type);

    <T extends IPipelineData> CompletableFuture<Boolean> exist(@NotNull Class<? extends T> type, @NotNull UUID uuid);

    <T extends IPipelineData> CompletableFuture<Boolean> delete(@NotNull Class<? extends T> type, @NotNull UUID uuid);

    <T extends IPipelineData> @NotNull PipelineLock<T> createPipelineLock(@NotNull Class<? extends T> dataClass, @NotNull UUID uuid);

    <T extends IPipelineData> @NotNull DataReference<T> createDataReference(@NotNull Class<? extends T> dataClass, @NotNull UUID uuid);

    <T extends IPipelineData> CompletableFuture<Boolean> saveAndRemoveFromLocalCache(@NotNull Class<? extends T> dataClass, @NotNull UUID uuid);
}
