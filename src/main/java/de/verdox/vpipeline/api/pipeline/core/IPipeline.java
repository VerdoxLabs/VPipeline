package de.verdox.vpipeline.api.pipeline.core;

import de.verdox.vpipeline.api.pipeline.datatypes.IDataRegistry;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import de.verdox.vpipeline.api.pipeline.datatypes.ISynchronizingService;
import de.verdox.vpipeline.api.pipeline.parts.IGlobalCache;
import de.verdox.vpipeline.api.pipeline.parts.IGlobalStorage;
import de.verdox.vpipeline.api.pipeline.parts.ILocalCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 11:22
 */
public interface IPipeline extends SystemPart {

    ILocalCache getLocalCache();

    @Nullable
    ISynchronizingService getSynchronizingService();

    @Nullable
    IGlobalCache getGlobalCache();

    @Nullable
    IGlobalStorage getGlobalStorage();

    @NotNull IDataRegistry getDataRegistry();

    void saveAll();

    void preloadAll();

    @Nullable <T extends IPipelineData> T load(@NotNull Class<? extends T> type, @NotNull UUID uuid, @NotNull LoadingStrategy loadingStrategy, boolean createIfNotExist, @Nullable Consumer<T> callback);

    @NotNull <T extends IPipelineData> Set<T> loadAllData(@NotNull Class<? extends T> type, @NotNull LoadingStrategy loadingStrategy);

    <T extends IPipelineData> boolean exist(@NotNull Class<? extends T> type, @NotNull UUID uuid, @NotNull QueryStrategy... strategies);

    <T extends IPipelineData> boolean delete(@NotNull Class<? extends T> type, @NotNull UUID uuid, boolean notifyOthers, @NotNull QueryStrategy... strategies);

    @Nullable
    default <T extends IPipelineData> T load(@NotNull Class<? extends T> type, @NotNull UUID uuid, @NotNull LoadingStrategy loadingStrategy) {
        return load(type, uuid, loadingStrategy, false, null);
    }

    @Nullable
    default <T extends IPipelineData> T load(@NotNull Class<? extends T> type, @NotNull UUID uuid, @NotNull LoadingStrategy loadingStrategy, boolean createIfNotExist) {
        return load(type, uuid, loadingStrategy, createIfNotExist, null);
    }

    @Nullable
    default <T extends IPipelineData> T load(@NotNull Class<? extends T> type, @NotNull UUID uuid, @NotNull LoadingStrategy loadingStrategy, @Nullable Consumer<T> callback) {
        return load(type, uuid, loadingStrategy, false, callback);
    }

    default <T extends IPipelineData> boolean delete(@NotNull Class<? extends T> type, @NotNull UUID uuid, @NotNull QueryStrategy... strategies) {
        return delete(type, uuid, true, strategies);
    }

    default <T extends IPipelineData> boolean delete(@NotNull Class<? extends T> type, @NotNull UUID uuid, boolean notifyOthers) {
        return delete(type, uuid, notifyOthers, QueryStrategy.ALL);
    }

    default <T extends IPipelineData> boolean delete(@NotNull Class<? extends T> type, @NotNull UUID uuid) {
        return delete(type, uuid, true, QueryStrategy.ALL);
    }

    IPipelineSynchronizer getPipelineSynchronizer();

    enum LoadingStrategy {
        // Data will be loaded from Local Cache
        LOAD_LOCAL,
        // Data will be loaded from local Cache if not cached it will be loaded into local cache async for the next possible try
        LOAD_LOCAL_ELSE_LOAD,
        // Loads data from PipeLine
        LOAD_PIPELINE,
    }

    enum QueryStrategy {
        // Instruction will be executed for Local Cache
        LOCAL,
        // Instruction will be executed for Global Cache
        GLOBAL_CACHE,
        // Instruction will be executed for Global Storage
        GLOBAL_STORAGE,
        // Instruction will be executed for all
        ALL
    }
}
