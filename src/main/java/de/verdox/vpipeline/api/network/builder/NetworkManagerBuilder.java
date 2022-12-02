package de.verdox.vpipeline.api.network.builder;

import de.verdox.vpipeline.api.messaging.MessagingService;
import de.verdox.vpipeline.api.network.NetworkManager;
import de.verdox.vpipeline.api.pipeline.core.Pipeline;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 03.07.2022 23:49
 */
public interface NetworkManagerBuilder {
    NetworkManagerBuilder withPipeline(Pipeline pipeline);

    NetworkManagerBuilder withMessagingService(MessagingService iMessagingService);

    NetworkManager buildNetworkManager();

}
