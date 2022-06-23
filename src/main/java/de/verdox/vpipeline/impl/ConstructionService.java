package de.verdox.vpipeline.impl;

import de.verdox.vpipeline.api.IConstructionService;
import de.verdox.vpipeline.api.messaging.IMessagingServiceBuilder;
import de.verdox.vpipeline.api.pipeline.IPipelineBuilder;
import de.verdox.vpipeline.impl.messaging.MessagingServiceBuilder;
import de.verdox.vpipeline.impl.pipeline.PipelineBuilder;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 19:24
 */
public class ConstructionService implements IConstructionService {

    @Override
    public IPipelineBuilder createPipelineConstructor() {
        return new PipelineBuilder();
    }

    @Override
    public IMessagingServiceBuilder createMessagingServiceBuilder() {
        return new MessagingServiceBuilder();
    }
}
