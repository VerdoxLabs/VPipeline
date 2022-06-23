package de.verdox.vpipeline.api.messaging;

import de.verdox.vpipeline.api.messaging.message.IMessage;
import de.verdox.vpipeline.api.pipeline.core.SystemPart;

import java.util.UUID;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 19.06.2022 12:25
 */
public interface ITransmitter extends SystemPart {
    void sendMessage(IMessage message, UUID... receivers);

    void broadcastMessage(IMessage message);

    void setMessagingService(IMessagingService messagingService);
}
