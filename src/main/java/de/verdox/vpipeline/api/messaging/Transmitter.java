package de.verdox.vpipeline.api.messaging;

import de.verdox.vpipeline.api.messaging.message.Message;
import de.verdox.vpipeline.api.pipeline.core.SystemPart;

import java.util.UUID;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 19.06.2022 12:25
 */
public interface Transmitter extends SystemPart {
    void sendMessage(Message message, UUID... receivers);

    void broadcastMessage(Message message);

    void setMessagingService(MessagingService messagingService);
}
