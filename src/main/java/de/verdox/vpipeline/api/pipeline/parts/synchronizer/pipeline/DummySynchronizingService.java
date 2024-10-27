package de.verdox.vpipeline.api.pipeline.parts.synchronizer.pipeline;

import de.verdox.vpipeline.api.pipeline.core.Pipeline;
import de.verdox.vpipeline.api.pipeline.datatypes.DataSynchronizer;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import de.verdox.vpipeline.api.pipeline.datatypes.PipelineData;
import de.verdox.vpipeline.api.pipeline.datatypes.SynchronizingService;
import org.jetbrains.annotations.NotNull;

public class DummySynchronizingService implements SynchronizingService {
    @Override
    public void connect() {

    }

    @Override
    public void disconnect() {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public DataSynchronizer getOrCreate(@NotNull Pipeline pipeline, @NotNull Class<? extends IPipelineData> type) {
        return new PipelineData.DummyDataDataSynchronizer(pipeline, type);
    }
}
