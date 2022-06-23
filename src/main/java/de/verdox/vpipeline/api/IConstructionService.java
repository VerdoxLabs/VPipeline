package de.verdox.vpipeline.api;

import de.verdox.vpipeline.api.messaging.IMessagingServiceBuilder;
import de.verdox.vpipeline.api.pipeline.IPipelineBuilder;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 19:21
 */
public interface IConstructionService {
    IPipelineBuilder createPipelineConstructor();
    IMessagingServiceBuilder createMessagingServiceBuilder();
}
