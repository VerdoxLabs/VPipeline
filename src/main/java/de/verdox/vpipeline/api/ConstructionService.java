package de.verdox.vpipeline.api;

import de.verdox.vpipeline.api.messaging.MessagingService;
import de.verdox.vpipeline.api.messaging.builder.MessagingServiceBuilder;
import de.verdox.vpipeline.api.network.NetworkManager;
import de.verdox.vpipeline.api.network.builder.NetworkManagerBuilder;
import de.verdox.vpipeline.api.pipeline.builder.PipelineBuilder;
import de.verdox.vpipeline.api.pipeline.core.Pipeline;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 19:21
 */
public interface ConstructionService {
    /**
     * Used to create a new Pipeline
     *
     * @return PipelineBuilder
     */
    PipelineBuilder createPipeline();

    /**
     * Used to create a new MessagingService
     *
     * @return MessagingServiceBuilder
     */
    MessagingServiceBuilder createMessagingService();

    /**
     * Used to create a new NetworkManager
     *
     * @return NetworkManagerBuilder
     */

    NetworkManagerBuilder createNetworkManager();

    /**
     * Used to create a new NetworkParticipant
     *
     * @return NetworkParticipant
     */

    NetworkParticipant createNetworkParticipant(Pipeline pipeline, MessagingService messagingService, NetworkManager networkManager);
}
