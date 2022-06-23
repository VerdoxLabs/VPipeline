package de.verdox.vpipeline.impl.messaging.message;

import de.verdox.vpipeline.api.messaging.message.IMessage;

import java.util.UUID;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 19.06.2022 12:48
 */
public record SimpleMessage(UUID sender, String senderIdentifier,
                            String[] parameters, Object[] dataToSend) implements IMessage {
    @Override
    public UUID getSender() {
        return sender;
    }

    @Override
    public String getSenderIdentifier() {
        return senderIdentifier;
    }

    @Override
    public String[] getParameters() {
        return parameters;
    }

    @Override
    public Object[] dataToSend() {
        return dataToSend;
    }
}
