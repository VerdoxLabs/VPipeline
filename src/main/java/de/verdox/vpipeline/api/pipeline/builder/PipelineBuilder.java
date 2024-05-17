package de.verdox.vpipeline.api.pipeline.builder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.verdox.vpipeline.api.pipeline.core.Pipeline;
import de.verdox.vpipeline.api.pipeline.datatypes.SynchronizingService;
import de.verdox.vpipeline.api.pipeline.parts.GlobalCache;
import de.verdox.vpipeline.api.pipeline.parts.GlobalStorage;

import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public interface PipelineBuilder {
    PipelineBuilder withGlobalCache(GlobalCache globalCache);

    PipelineBuilder withGlobalStorage(GlobalStorage globalStorage);

    PipelineBuilder withSynchronizingService(SynchronizingService synchronizingService);

    PipelineBuilder withExecutorService(ExecutorService executorService);

    PipelineBuilder withGson(Consumer<GsonBuilder> gsonBuilderConsumer);

    Pipeline buildPipeline();
}
