package de.verdox.vpipeline.impl;

import de.verdox.vpipeline.api.messaging.MessagingService;
import de.verdox.vpipeline.api.network.NetworkManager;
import de.verdox.vpipeline.api.pipeline.core.Pipeline;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 04.07.2022 00:13
 */
public record NetworkParticipantImpl(Pipeline pipeline, MessagingService messagingService,
                                     NetworkManager networkManager) implements de.verdox.vpipeline.api.NetworkParticipant {

}
