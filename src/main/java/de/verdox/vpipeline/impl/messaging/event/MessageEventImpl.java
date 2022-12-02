package de.verdox.vpipeline.impl.messaging.event;

import de.verdox.vpipeline.api.messaging.event.MessageEvent;
import de.verdox.vpipeline.api.messaging.message.Message;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 19.06.2022 13:30
 */
public record MessageEventImpl(String channelName, Message message) implements MessageEvent {
    @Override
    public String getChannelName() {
        return channelName;
    }

    @Override
    public Message getMessage() {
        return message;
    }
}
