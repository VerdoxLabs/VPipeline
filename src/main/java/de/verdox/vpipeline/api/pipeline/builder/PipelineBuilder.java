package de.verdox.vpipeline.api.pipeline.builder;

import com.google.gson.GsonBuilder;
import de.verdox.vpipeline.api.pipeline.core.NetworkDataLockingService;
import de.verdox.vpipeline.api.pipeline.core.Pipeline;
import de.verdox.vpipeline.api.pipeline.datatypes.SynchronizingService;
import de.verdox.vpipeline.api.pipeline.parts.GlobalCache;
import de.verdox.vpipeline.api.pipeline.parts.GlobalStorage;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Used to build a pipeline
 */
public interface PipelineBuilder {
    /**
     * Used to specify the {@link GlobalCache} that should be used
     *
     * @param globalCache the global cache
     * @return the builder
     */
    PipelineBuilder withGlobalCache(GlobalCache globalCache);

    /**
     * Used to specify the {@link GlobalStorage} that should be used
     *
     * @param globalStorage the global storage
     * @return the builder
     */
    PipelineBuilder withGlobalStorage(GlobalStorage globalStorage);

    /**
     * Used to specify the {@link SynchronizingService} that should be used
     *
     * @param synchronizingService the synchronizingService
     * @return the builder
     */
    PipelineBuilder withSynchronizingService(SynchronizingService synchronizingService);

    /**
     * Used to specify the {@link NetworkDataLockingService} that should be used
     *
     * @param networkDataLockingService the networkDataLockingService
     * @return the builder
     */
    PipelineBuilder withNetworkDataLockingService(@NotNull NetworkDataLockingService networkDataLockingService);

    /**
     * Used to specify any gson parser settings that are applied for serialization purposes
     *
     * @param gsonBuilderConsumer the consumer
     * @return the builder
     */
    PipelineBuilder withGson(Consumer<GsonBuilder> gsonBuilderConsumer);

    /**
     * Builds the pipeline
     *
     * @return the pipeline
     */
    Pipeline buildPipeline();
}
