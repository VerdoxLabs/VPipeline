package de.verdox.vpipeline.impl.pipeline.core;

import de.verdox.vpipeline.api.pipeline.core.PipelineTask;
import de.verdox.vpipeline.api.pipeline.core.PipelineTaskScheduler;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PipelineTaskImpl<T extends IPipelineData> implements PipelineTask<T> {

    private final Class<? extends IPipelineData> type;
    private final UUID uuid;
    private final PipelineTaskScheduler.TaskType taskType;
    private final CompletableFuture<T> futureLoadedObject;
    private final UUID taskUUID = UUID.randomUUID();

    public PipelineTaskImpl(Class<? extends IPipelineData> type, UUID uuid, PipelineTaskScheduler.TaskType taskType, CompletableFuture<T> futureLoadedObject) {
        this.type = type;
        this.uuid = uuid;
        this.taskType = taskType;
        this.futureLoadedObject = futureLoadedObject;
    }


    @Override
    public UUID getTaskUUID() {
        return taskUUID;
    }

    @Override
    public UUID getDataUUID() {
        return uuid;
    }

    @Override
    public PipelineTaskScheduler.TaskType getTaskType() {
        return taskType;
    }

    @Override
    public CompletableFuture<T> getFutureObject() {
        return futureLoadedObject;
    }
}
