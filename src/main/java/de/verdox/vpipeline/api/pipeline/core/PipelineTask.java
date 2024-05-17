package de.verdox.vpipeline.api.pipeline.core;

import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Deprecated
public interface PipelineTask<T extends IPipelineData> {
    UUID getTaskUUID();

    UUID getDataUUID();

    PipelineTaskScheduler.TaskType getTaskType();

    CompletableFuture<T> getFutureObject();
}
