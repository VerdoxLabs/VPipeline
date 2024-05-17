package de.verdox.vpipeline.api.messaging.event;

import de.verdox.vpipeline.api.messaging.instruction.Instruction;

/**
 * Event that is called whenever a message is received through a Transmitter.
 */
public interface MessageEvent  {
    String getChannelName();
    Instruction<?> getMessage();
}
