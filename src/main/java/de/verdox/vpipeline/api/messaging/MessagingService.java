package de.verdox.vpipeline.api.messaging;

import com.google.gson.GsonBuilder;
import de.verdox.vserializer.json.JsonSerializer;
import de.verdox.vserializer.json.JsonSerializerBuilder;
import de.verdox.vserializer.SerializableField;
import de.verdox.vpipeline.api.Connection;
import de.verdox.vpipeline.api.NetworkParticipant;
import de.verdox.vpipeline.api.messaging.instruction.Instruction;
import de.verdox.vpipeline.api.messaging.instruction.ResponseCollector;
import de.verdox.vpipeline.api.pipeline.core.Pipeline;
import de.verdox.vpipeline.api.pipeline.datatypes.SynchronizingService;
import de.verdox.vpipeline.api.pipeline.parts.GlobalCache;
import de.verdox.vpipeline.api.pipeline.parts.GlobalStorage;
import de.verdox.vpipeline.api.pipeline.parts.NetworkDataLockingService;
import de.verdox.vpipeline.api.pipeline.parts.cache.local.HashedLocalCache;
import de.verdox.vpipeline.api.ticket.TicketPropagator;
import de.verdox.vpipeline.impl.messaging.MessagingServiceImpl;
import de.verdox.vpipeline.impl.messaging.ResponseCollectorImpl;
import de.verdox.vpipeline.api.pipeline.core.SystemPart;
import de.verdox.vpipeline.impl.pipeline.core.PipelineImpl;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public interface MessagingService extends SystemPart, Connection {

    JsonSerializer<MessagingService> SERIALIZER = JsonSerializerBuilder.create("messagingService", MessagingService.class)
            .constructor(
                    new SerializableField<>("identifier", JsonSerializer.Primitive.STRING, MessagingService::getSessionIdentifier),
                    new SerializableField<>("transmitter", Transmitter.SERIALIZER, MessagingService::getTransmitter),
                    MessagingServiceImpl::new
            )
            .build();

    default void connect(){
        getTransmitter().connect();
        sendKeepAlivePing();
    }

    default void disconnect(){
        getTransmitter().disconnect();
    }

    NetworkParticipant getNetworkParticipant();
    Set<RemoteMessageReceiver> getRemoteMessageReceivers();
    void sendKeepAlivePing();
    Transmitter getTransmitter();
    default UUID getSessionUUID(){
        return getNetworkParticipant().getUUID();
    }
    String getSessionIdentifier();
    MessageFactory getMessageFactory();
    TicketPropagator getTicketPropagator();

    void postMessageEvent(String channel, Instruction<?> instruction);

    <R, T extends Instruction<R>> ResponseCollector<R> sendInstruction(@NotNull T instruction, UUID... receivers);
    <R, T extends Instruction<R>> ResponseCollector<R> sendInstruction(@NotNull Class<? extends T> instructionType, Consumer<T> consumer, UUID... receivers);
}