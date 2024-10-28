package de.verdox.vpipeline.api.messaging;

import de.verdox.vpipeline.api.Connection;
import de.verdox.vpipeline.api.NetworkParticipant;
import de.verdox.vpipeline.api.messaging.instruction.Instruction;
import de.verdox.vpipeline.api.messaging.instruction.ResponseCollector;
import de.verdox.vpipeline.api.pipeline.core.SystemPart;
import de.verdox.vpipeline.api.ticket.TicketPropagator;
import de.verdox.vpipeline.impl.messaging.MessagingServiceImpl;
import de.verdox.vserializer.SerializableField;
import de.verdox.vserializer.generic.Serializer;
import de.verdox.vserializer.generic.SerializerBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public interface MessagingService extends SystemPart, Connection {

    Serializer<MessagingService> SERIALIZER = SerializerBuilder.create("messagingService", MessagingService.class)
            .constructor(
                    new SerializableField<>("identifier", Serializer.Primitive.STRING, MessagingService::getSessionIdentifier),
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