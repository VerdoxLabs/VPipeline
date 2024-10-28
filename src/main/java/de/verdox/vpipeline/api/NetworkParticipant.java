package de.verdox.vpipeline.api;

import de.verdox.vserializer.generic.Serializer;
import de.verdox.vserializer.generic.SerializerBuilder;
import de.verdox.vserializer.SerializableField;
import de.verdox.vpipeline.api.messaging.MessagingService;
import de.verdox.vpipeline.api.messaging.RemoteMessageReceiver;
import de.verdox.vpipeline.api.network.RemoteParticipant;
import de.verdox.vpipeline.api.pipeline.core.Pipeline;
import de.verdox.vpipeline.api.pipeline.parts.cache.local.DataAccess;
import de.verdox.vpipeline.impl.network.NetworkParticipantImpl;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;

public interface NetworkParticipant extends Connection {

    Serializer<NetworkParticipant> SERIALIZER = SerializerBuilder.create("participant", NetworkParticipant.class)
            .constructor(
                    new SerializableField<>("identifier", Serializer.Primitive.STRING, NetworkParticipant::getIdentifier),
                    new SerializableField<>("pipeline", Pipeline.SERIALIZER, NetworkParticipant::pipeline),
                    new SerializableField<>("messagingService", MessagingService.SERIALIZER, NetworkParticipant::messagingService),
                    (s, pipeline, messagingService) -> new NetworkParticipantImpl(UUID.nameUUIDFromBytes(s.getBytes(StandardCharsets.UTF_8)), s, pipeline, messagingService, Executors.newSingleThreadScheduledExecutor())
            ).build();

    default void connect(){
        pipeline().connect();
        messagingService().connect();
    }

    default void disconnect(){
        pipeline().disconnect();
        messagingService().disconnect();
    }

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

    default Set<RemoteMessageReceiver> getRemoteAliveMessagingParticipants(){
        return messagingService().getRemoteMessageReceivers();
    }

    UUID getUUID();

    String getIdentifier();

    void shutdown();

}
