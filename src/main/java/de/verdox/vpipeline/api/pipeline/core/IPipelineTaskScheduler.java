package de.verdox.vpipeline.api.pipeline.core;

import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 11:05
 */
public interface IPipelineTaskScheduler extends SystemPart{
    <T extends IPipelineData> IPipelineTask<T> schedulePipelineTask(@NotNull Class<? extends T> type, @NotNull UUID uuid);
}
