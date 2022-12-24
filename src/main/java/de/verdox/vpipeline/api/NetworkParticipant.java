package de.verdox.vpipeline.api;

import de.verdox.vpipeline.api.messaging.MessagingService;
import de.verdox.vpipeline.api.network.RemoteParticipant;
import de.verdox.vpipeline.api.pipeline.core.Pipeline;
import de.verdox.vpipeline.api.pipeline.core.PipelineLock;
import de.verdox.vpipeline.api.pipeline.datatypes.customtypes.DataReference;
import de.verdox.vpipeline.impl.network.RemoteParticipantImpl;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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

    CompletableFuture<Set<DataReference<RemoteParticipant>>> getOnlineNetworkClients();

    DataReference<RemoteParticipant> getOnlineNetworkClient(String identifier);

    DataReference<RemoteParticipant> getAsNetworkClient();

    UUID getUUID();

    String getIdentifier();

    void shutdown();

}
