package de.verdox.vpipeline.impl.pipeline.core;

import de.verdox.vpipeline.api.pipeline.core.IPipelineTask;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 11:45
 */
public class PipelineTask <T extends IPipelineData> implements IPipelineTask<T> {

    private final Class<? extends IPipelineData> type;
    private final UUID uuid;
    private final CompletableFuture<T> futureLoadedObject;
    private final UUID taskUUID = UUID.randomUUID();

    public PipelineTask(Class<? extends IPipelineData> type, UUID uuid, CompletableFuture<T> futureLoadedObject) {
        this.type = type;
        this.uuid = uuid;
        this.futureLoadedObject = futureLoadedObject;
    }


    @Override
    public UUID getTaskUUID() {
        return null;
    }

    @Override
    public CompletableFuture<T> getFutureObject() {
        return futureLoadedObject;
    }
}
