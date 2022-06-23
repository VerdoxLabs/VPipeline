package de.verdox.vpipeline.impl.messaging.event;

import de.verdox.vpipeline.api.messaging.event.IMessageEvent;
import de.verdox.vpipeline.api.messaging.message.IMessage;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 19.06.2022 13:30
 */
public record MessageEvent(String channelName, IMessage message) implements IMessageEvent {
    @Override
    public String getChannelName() {
        return channelName;
    }

    @Override
    public IMessage getMessage() {
        return message;
    }
}
