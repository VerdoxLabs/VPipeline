package de.verdox.vpipeline.impl.pipeline.builder;

import com.google.gson.GsonBuilder;
import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.pipeline.builder.PipelineBuilder;
import de.verdox.vpipeline.api.pipeline.core.NetworkDataLockingService;
import de.verdox.vpipeline.api.pipeline.core.Pipeline;
import de.verdox.vpipeline.api.pipeline.datatypes.SynchronizingService;
import de.verdox.vpipeline.api.pipeline.parts.GlobalCache;
import de.verdox.vpipeline.api.pipeline.parts.GlobalStorage;
import de.verdox.vpipeline.api.pipeline.parts.LocalCache;
import de.verdox.vpipeline.impl.pipeline.core.PipelineImpl;
import de.verdox.vpipeline.api.pipeline.parts.cache.local.HashedLocalCache;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class PipelineBuilderImpl implements PipelineBuilder {
    private GlobalCache globalCache;
    private GlobalStorage globalStorage;
    private SynchronizingService synchronizingService;
    private NetworkDataLockingService networkDataLockingService;

    private Consumer<GsonBuilder> gsonBuilderConsumer;
    private LocalCache localCache = new HashedLocalCache();

    public PipelineBuilder withLocalCache(LocalCache localCache){
        this.localCache = localCache;
        return this;
    }

    @Override
    public PipelineBuilder withGlobalCache(GlobalCache globalCache) {
        checkCache();
        this.globalCache = globalCache;
        return this;
    }

    @Override
    public PipelineBuilder withGlobalStorage(GlobalStorage globalStorage) {
        checkStorage();
        this.globalStorage = globalStorage;
        return this;
    }

    @Override
    public PipelineBuilder withNetworkDataLockingService(@NotNull NetworkDataLockingService networkDataLockingService){
        checkNetworkDataLockingService();
        this.networkDataLockingService = networkDataLockingService;
        return this;
    }

    @Override
    public PipelineBuilder withSynchronizingService(SynchronizingService synchronizingService) {
        checkSynchronizingService();
        this.synchronizingService = synchronizingService;
        return this;
    }

    @Override
    public PipelineBuilder withGson(Consumer<GsonBuilder> gsonBuilderConsumer) {
        this.gsonBuilderConsumer = gsonBuilderConsumer;
        return this;
    }

    @Override
    public Pipeline buildPipeline() {
        if (globalStorage == null && globalCache == null)
            NetworkLogger.warning("Both globalCache and globalStorage were not set during pipeline building phase.");
        if (synchronizingService == null && globalCache != null)
            NetworkLogger.warning("A globalCache but no synchronizing service was set during pipeline building phase.");
        return new PipelineImpl(localCache, networkDataLockingService, globalCache, globalStorage, synchronizingService, gsonBuilderConsumer);
    }

    private void checkStorage() {
        if (globalStorage != null)
            throw new RuntimeException("GlobalStorage already set in PipelineBuilder");
    }

    private void checkCache() {
        if (globalCache != null)
            throw new RuntimeException("GlobalCache already set in PipelineBuilder");
    }

    private void checkSynchronizingService() {
        if (synchronizingService != null)
            throw new RuntimeException("GlobalCache already set in PipelineBuilder");
    }

    private void checkNetworkDataLockingService() {
        if (networkDataLockingService != null)
            throw new RuntimeException("NetworkDataLockingService already set in PipelineBuilder");
    }
}
