package de.verdox.vpipeline.api.messaging;

import de.verdox.vpipeline.api.messaging.instruction.Instruction;
import de.verdox.vpipeline.api.pipeline.core.SystemPart;

import java.util.UUID;

public interface Transmitter extends SystemPart {
    long sendMessage(Instruction<?> message, UUID... receivers);

    long broadcastMessage(Instruction<?> message);

    void setMessagingService(MessagingService messagingService);

    long getNetworkTransmitterAmount();
}
