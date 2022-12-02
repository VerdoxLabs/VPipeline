package de.verdox.vpipeline.impl;

import de.verdox.vpipeline.api.ConstructionService;
import de.verdox.vpipeline.api.NetworkParticipant;
import de.verdox.vpipeline.api.messaging.MessagingService;
import de.verdox.vpipeline.api.messaging.builder.MessagingServiceBuilder;
import de.verdox.vpipeline.api.network.NetworkManager;
import de.verdox.vpipeline.api.network.builder.NetworkManagerBuilder;
import de.verdox.vpipeline.api.pipeline.builder.PipelineBuilder;
import de.verdox.vpipeline.api.pipeline.core.Pipeline;
import de.verdox.vpipeline.impl.messaging.MessagingServiceImpl;
import de.verdox.vpipeline.impl.messaging.builder.MessagingServiceBuilderImpl;
import de.verdox.vpipeline.impl.network.NetworkManagerImpl;
import de.verdox.vpipeline.impl.network.builder.NetworkManagerBuilderImpl;
import de.verdox.vpipeline.impl.pipeline.builder.PipelineBuilderImpl;
import de.verdox.vpipeline.impl.pipeline.core.PipelineImpl;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 19:24
 */
public class ConstructionServiceImpl implements ConstructionService {

    @Override
    public PipelineBuilder createPipeline() {
        return new PipelineBuilderImpl();
    }

    @Override
    public MessagingServiceBuilder createMessagingService() {
        return new MessagingServiceBuilderImpl();
    }

    @Override
    public NetworkManagerBuilder createNetworkManager() {
        return new NetworkManagerBuilderImpl();
    }

    @Override
    public NetworkParticipant createNetworkParticipant(Pipeline pipeline, MessagingService messagingService, NetworkManager networkManager) {
        NetworkParticipantImpl networkParticipant = new NetworkParticipantImpl(pipeline, messagingService, networkManager);
        if (pipeline instanceof PipelineImpl pipelineImpl)
            pipelineImpl.setNetworkParticipant(networkParticipant);
        if (messagingService instanceof MessagingServiceImpl messagingServiceImpl)
            messagingServiceImpl.setNetworkParticipant(networkParticipant);
        if (networkManager instanceof NetworkManagerImpl networkManagerImpl)
            networkManagerImpl.setNetworkParticipant(networkParticipant);
        return networkParticipant;
    }
}