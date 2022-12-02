package de.verdox.vpipeline.api.pipeline.core;

import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 11:04
 */
public interface PipelineSynchronizer extends SystemPart {

    boolean synchronize(@NotNull DataSourceType source, @NotNull DataSourceType destination, @NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID, Runnable callback);
    default boolean synchronize(@NotNull DataSourceType source, @NotNull DataSourceType destination, @NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID) {
        return synchronize(source, destination, dataClass, objectUUID, null);
    }

    enum DataSourceType {
        LOCAL,
        GLOBAL_CACHE,
        GLOBAL_STORAGE
    }
}
