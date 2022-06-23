package de.verdox.vpipeline.api.messaging;

import de.verdox.vpipeline.api.messaging.instruction.IInstruction;
import de.verdox.vpipeline.api.messaging.message.IMessage;
import de.verdox.vpipeline.api.pipeline.core.SystemPart;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 23:10
 */
public interface IMessagingService extends SystemPart {

    String INSTRUCTION_IDENTIFIER = "VInstruction";
    String RESPONSE_IDENTIFIER = "VResponse";

    void sendInstruction(@NotNull IInstruction<?> instruction);
    void sendInstruction(@NotNull IInstruction<?> instruction, UUID... receivers);
    ITransmitter getTransmitter();
    IMessageFactory getMessageFactory();
    UUID getSessionUUID();
    String getSessionIdentifier();
    boolean isOwnMessage(IMessage message);

    void postMessageEvent(String channelName, IMessage message);
}