package de.verdox.vpipeline.api.messaging;

import de.verdox.vpipeline.api.NetworkParticipant;
import de.verdox.vpipeline.api.messaging.instruction.Instruction;
import de.verdox.vpipeline.api.messaging.instruction.types.Query;
import de.verdox.vpipeline.api.messaging.instruction.types.Response;
import de.verdox.vpipeline.api.messaging.message.Message;
import de.verdox.vpipeline.api.pipeline.core.SystemPart;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 23:10
 */
public interface MessagingService extends SystemPart {

    NetworkParticipant getNetworkParticipant();

    String INSTRUCTION_IDENTIFIER = "VInstruction";
    String RESPONSE_IDENTIFIER = "VResponse";

    default <T> Response<T> sendInstruction(@NotNull Class<? extends Instruction<T>> type, Consumer<Instruction<T>> instructionModifier) {
        return sendInstruction(type, instructionModifier, new UUID[0]);
    }

    default <T> Response<T> sendInstruction(@NotNull Class<? extends Instruction<T>> type, Consumer<Instruction<T>> instructionModifier, UUID... receivers) {
        var instructionID = getMessageFactory().findInstructionID(type);
        if (instructionID == -1)
            throw new IllegalStateException("Instruction type " + type.getSimpleName() + " not registered");
        var createdInstance = type.cast(getMessageFactory().getInstructionType(instructionID).instanceSupplier().get());
        instructionModifier.accept(createdInstance);
        return sendInstruction(createdInstance, receivers);
    }

    <T> Response<T> sendInstruction(@NotNull Instruction<T> instruction);

    <T> Response<T> sendInstruction(@NotNull Instruction<T> instruction, UUID... receivers);

    Transmitter getTransmitter();

    MessageFactory getMessageFactory();

    UUID getSessionUUID();

    String getSessionIdentifier();

    boolean isOwnMessage(Message message);

    void postMessageEvent(String channelName, Message message);

    Set<RemoteMessageReceiver> getRemoteMessageReceivers();

    void sendKeepAlivePing();
}