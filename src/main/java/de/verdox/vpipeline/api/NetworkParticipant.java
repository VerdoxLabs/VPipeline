package de.verdox.vpipeline.api;

import de.verdox.vpipeline.api.messaging.MessagingService;
import de.verdox.vpipeline.api.network.NetworkManager;
import de.verdox.vpipeline.api.pipeline.core.Pipeline;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 04.07.2022 00:13
 */
public interface NetworkParticipant {

    /**
     * @return The pipeline of this NetworkParticipant
     */
    Pipeline pipeline();

    /**
     * @return The MessagingService of this NetworkParticipant
     */
    MessagingService messagingService();

    /**
     * @return The NetworkManager of this NetworkParticipant
     */
    NetworkManager networkManager();

}
