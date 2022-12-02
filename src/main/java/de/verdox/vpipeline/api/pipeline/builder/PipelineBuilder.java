package de.verdox.vpipeline.api.pipeline.builder;

import de.verdox.vpipeline.api.pipeline.core.Pipeline;
import de.verdox.vpipeline.api.pipeline.datatypes.SynchronizingService;
import de.verdox.vpipeline.api.pipeline.parts.GlobalCache;
import de.verdox.vpipeline.api.pipeline.parts.GlobalStorage;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 14:35
 */
public interface PipelineBuilder {
    PipelineBuilder withGlobalCache(GlobalCache globalCache);

    PipelineBuilder withGlobalStorage(GlobalStorage globalStorage);

    PipelineBuilder withSynchronizingService(SynchronizingService synchronizingService);

    Pipeline buildPipeline();
}
