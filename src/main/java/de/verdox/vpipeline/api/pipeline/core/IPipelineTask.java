package de.verdox.vpipeline.api.pipeline.core;

import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 11:05
 */
public interface IPipelineTask<T extends IPipelineData> {
    UUID getTaskUUID();
    CompletableFuture<T> getFutureObject();
}
