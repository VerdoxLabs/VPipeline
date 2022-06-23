package de.verdox.vpipeline.api.pipeline.datatypes;

import de.verdox.vpipeline.api.pipeline.core.IPipeline;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 11:28
 */
public interface ISynchronizingService {
    ISynchronizer getSynchronizer(IPipeline pipeline, IPipelineData data);
}