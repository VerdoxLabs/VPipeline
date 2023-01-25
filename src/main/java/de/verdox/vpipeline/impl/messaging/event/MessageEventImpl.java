package de.verdox.vpipeline.impl.messaging.event;

import de.verdox.vpipeline.api.messaging.event.MessageEvent;
import de.verdox.vpipeline.api.messaging.instruction.Instruction;


/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 19.06.2022 13:30
 */
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
