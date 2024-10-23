package de.verdox.vpipeline.api.messaging.event;

import de.verdox.vpipeline.api.messaging.instruction.Instruction;

/**
 * Event that is called whenever a message is received through a Transmitter.
 */
public interface MessageEvent  {
    /**
     * The name of the channel the message was sent to
     * @return the channel name
     */
    String getChannelName();

    /**
     * Returns the actual message
     * @return The message
     */
    Instruction<?> getMessage();
}
