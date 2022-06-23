package de.verdox.vpipeline.api.messaging.event;

import de.verdox.vpipeline.api.messaging.message.IMessage;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 19.06.2022 13:30
 */
public interface IMessageEvent {
    String getChannelName();
    IMessage getMessage();
}
