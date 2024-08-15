package de.verdox.vpipeline.api.pipeline.core;

/**
 * A part of the {@link Pipeline}
 */
public interface SystemPart {
    /**
     * Used to shut down this system part
     */
    void shutdown();
}
