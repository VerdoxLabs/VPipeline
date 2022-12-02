package de.verdox.vpipeline.api.pipeline.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.verdox.vpipeline.api.NetworkParticipant;
import de.verdox.vpipeline.api.pipeline.SynchronizedAccess;
import de.verdox.vpipeline.api.pipeline.datatypes.DataRegistry;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import de.verdox.vpipeline.api.pipeline.datatypes.SynchronizingService;
import de.verdox.vpipeline.api.pipeline.parts.GlobalCache;
import de.verdox.vpipeline.api.pipeline.parts.GlobalStorage;
import de.verdox.vpipeline.api.pipeline.parts.LocalCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface Pipeline extends SystemPart {
    NetworkParticipant getNetworkParticipant();

    LocalCache getLocalCache();

    @Nullable
    SynchronizingService getSynchronizingService();

    @NotNull
    PipelineSynchronizer getPipelineSynchronizer();

    @Nullable
    GlobalCache getGlobalCache();

    @Nullable
    GlobalStorage getGlobalStorage();

    @NotNull DataRegistry getDataRegistry();

    @NotNull GsonBuilder getGsonBuilder();

    void saveAll();

    void preloadAll();

    @NotNull <T extends IPipelineData> CompletableFuture<SynchronizedAccess<T>> load(@NotNull Class<? extends T> type, @NotNull UUID uuid);

    @NotNull <T extends IPipelineData> CompletableFuture<SynchronizedAccess<T>> loadOrCreate(@NotNull Class<? extends T> type, @NotNull UUID uuid);

    @NotNull <T extends IPipelineData> CompletableFuture<Set<SynchronizedAccess<T>>> loadAllData(@NotNull Class<? extends T> type);

    <T extends IPipelineData> CompletableFuture<Boolean> exist(@NotNull Class<? extends T> type, @NotNull UUID uuid);

    <T extends IPipelineData> CompletableFuture<Boolean> delete(@NotNull Class<? extends T> type, @NotNull UUID uuid);
}
