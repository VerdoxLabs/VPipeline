package de.verdox.vpipeline.api;

import de.verdox.vpipeline.api.pipeline.parts.cache.local.DataAccess;
import de.verdox.vpipeline.api.messaging.MessagingService;
import de.verdox.vpipeline.api.network.RemoteParticipant;
import de.verdox.vpipeline.api.pipeline.core.Pipeline;

import java.util.Set;
import java.util.UUID;

public interface NetworkParticipant {

    /**
     * @return The pipeline of this NetworkParticipant
     */
    Pipeline pipeline();

    /**
     * @return The MessagingService of this NetworkParticipant
     */
    MessagingService messagingService();

    Set<DataAccess<? extends RemoteParticipant>> getOnlineNetworkClients();

    DataAccess<RemoteParticipant> getOnlineNetworkClient(String identifier);

    DataAccess<RemoteParticipant> getAsNetworkClient();

    UUID getUUID();

    String getIdentifier();

    void shutdown();

}
