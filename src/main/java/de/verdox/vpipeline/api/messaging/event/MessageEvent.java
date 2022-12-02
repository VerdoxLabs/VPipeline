package de.verdox.vpipeline.api.messaging.event;

import de.verdox.vpipeline.api.messaging.message.Message;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 19.06.2022 13:30
 */

/**
 * Event that is called whenever a message is received through a Transmitter.
 */
public interface MessageEvent  {
    String getChannelName();
    Message getMessage();
}
