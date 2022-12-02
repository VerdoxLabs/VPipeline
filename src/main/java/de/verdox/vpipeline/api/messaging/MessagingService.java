package de.verdox.vpipeline.api.messaging;

import de.verdox.vpipeline.api.NetworkParticipant;
import de.verdox.vpipeline.api.messaging.instruction.Instruction;
import de.verdox.vpipeline.api.messaging.message.Message;
import de.verdox.vpipeline.api.pipeline.core.SystemPart;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 23:10
 */
public interface MessagingService extends SystemPart {

    NetworkParticipant getNetworkParticipant();

    String INSTRUCTION_IDENTIFIER = "VInstruction";
    String RESPONSE_IDENTIFIER = "VResponse";

    void sendInstruction(@NotNull Instruction<?> instruction);

    void sendInstruction(@NotNull Instruction<?> instruction, UUID... receivers);

    Transmitter getTransmitter();

    MessageFactory getMessageFactory();

    UUID getSessionUUID();

    String getSessionIdentifier();

    boolean isOwnMessage(Message message);

    void postMessageEvent(String channelName, Message message);
}