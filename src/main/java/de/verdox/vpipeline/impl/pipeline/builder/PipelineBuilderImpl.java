package de.verdox.vpipeline.impl.pipeline.builder;

import de.verdox.vpipeline.api.pipeline.builder.PipelineBuilder;
import de.verdox.vpipeline.api.pipeline.core.Pipeline;
import de.verdox.vpipeline.api.pipeline.datatypes.SynchronizingService;
import de.verdox.vpipeline.api.pipeline.parts.GlobalCache;
import de.verdox.vpipeline.api.pipeline.parts.GlobalStorage;
import de.verdox.vpipeline.impl.pipeline.core.PipelineImpl;

import java.util.logging.Logger;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 18:23
 */
public class PipelineBuilderImpl implements PipelineBuilder {
    static Logger log = Logger.getLogger(PipelineBuilderImpl.class.getName());

    private GlobalCache globalCache;
    private GlobalStorage globalStorage;
    private SynchronizingService synchronizingService;

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
    public PipelineBuilder withSynchronizingService(SynchronizingService synchronizingService) {
        checkSynchronizingService();
        this.synchronizingService = synchronizingService;
        return this;
    }

    @Override
    public Pipeline buildPipeline() {
        if (globalStorage == null && globalCache == null)
            log.warning("Both globalCache and globalStorage were not set during pipeline building phase.");
        if(synchronizingService == null && globalCache != null)
            log.warning("A globalCache but no synchronizing service was set during pipeline building phase.");
        return new PipelineImpl(globalCache, globalStorage, synchronizingService);
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
}
