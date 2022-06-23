package de.verdox.vpipeline.impl.messaging.message;

import de.verdox.vpipeline.api.messaging.message.IMessage;
import de.verdox.vpipeline.impl.messaging.MessageBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 19.06.2022 12:48
 */
public class SimpleMessageBuilder extends MessageBuilder {
    public SimpleMessageBuilder(@NotNull UUID sender, @NotNull String senderIdentifier) {
        super(sender, senderIdentifier);
    }

    @Override
    public IMessage constructMessage() {
        return new SimpleMessage(sender, senderIdentifier, parameters, dataToSend);
    }
}
