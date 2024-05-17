package de.verdox.vpipeline.impl.messaging.event;

import de.verdox.vpipeline.api.messaging.event.MessageEvent;
import de.verdox.vpipeline.api.messaging.instruction.Instruction;

public record MessageEventImpl(String channelName, Instruction<?> instruction) implements MessageEvent {
    @Override
    public String getChannelName() {
        return channelName;
    }

    @Override
    public Instruction<?> getMessage() {
        return instruction;
    }
}
