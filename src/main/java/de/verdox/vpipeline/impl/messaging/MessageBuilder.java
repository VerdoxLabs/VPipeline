package de.verdox.vpipeline.impl.messaging;

import de.verdox.vpipeline.api.messaging.message.IMessage;
import de.verdox.vpipeline.impl.messaging.message.SimpleMessage;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 23:00
 */
public class MessageBuilder {
    protected final UUID sender;
    protected final String senderIdentifier;
    protected String[] parameters;
    protected Object[] dataToSend;

    public MessageBuilder(@NotNull UUID sender, @NotNull String senderIdentifier) {
        Objects.requireNonNull(sender, "sender can't be null!");
        Objects.requireNonNull(senderIdentifier, "senderIdentifier can't be null!");
        this.sender = sender;
        this.senderIdentifier = senderIdentifier;
    }

    public MessageBuilder withParameters(String... parameters) {
        this.parameters = parameters;
        return this;
    }

    public MessageBuilder withData(Object... dataToSend) {
        this.dataToSend = dataToSend;
        return this;
    }

    public IMessage constructMessage() {
        return new SimpleMessage(sender, senderIdentifier, parameters, dataToSend);
    }
}
