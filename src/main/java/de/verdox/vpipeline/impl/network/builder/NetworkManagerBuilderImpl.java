package de.verdox.vpipeline.impl.network.builder;

import de.verdox.vpipeline.api.messaging.MessagingService;
import de.verdox.vpipeline.api.network.NetworkManager;
import de.verdox.vpipeline.api.network.builder.NetworkManagerBuilder;
import de.verdox.vpipeline.api.pipeline.core.Pipeline;
import de.verdox.vpipeline.impl.network.NetworkManagerImpl;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 03.07.2022 23:51
 */
public class NetworkManagerBuilderImpl implements NetworkManagerBuilder {

    private Pipeline pipeline;
    private MessagingService messagingService;

    @Override
    public NetworkManagerBuilder withPipeline(Pipeline pipeline) {
        this.pipeline = pipeline;
        return this;
    }

    @Override
    public NetworkManagerBuilder withMessagingService(MessagingService messagingService) {
        this.messagingService = messagingService;
        return this;
    }

    @Override
    public NetworkManager buildNetworkManager() {
        if (pipeline == null || messagingService == null)
            throw new NullPointerException("pipeline & messagingService can't be null while constructing network manager");
        return new NetworkManagerImpl(pipeline, messagingService);
    }
}
