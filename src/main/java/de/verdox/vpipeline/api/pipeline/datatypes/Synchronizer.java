package de.verdox.vpipeline.api.pipeline.datatypes;

import de.verdox.vpipeline.api.pipeline.core.SystemPart;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface Synchronizer extends SystemPart {
    /**
     * Cleanup Function triggered when data is removed from cache
     */
    void cleanUp();
    /**
     * Pushes the local data to Pipeline
     */
    void pushUpdate(@NotNull IPipelineData data, Runnable callback);

    /**
     * Notifies other Servers that hold this data to delete it from local Cache
     */
    void pushRemoval(@NotNull UUID uuid, Runnable callback);
}
