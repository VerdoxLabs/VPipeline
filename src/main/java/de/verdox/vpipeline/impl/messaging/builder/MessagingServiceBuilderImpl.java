package de.verdox.vpipeline.impl.messaging.builder;

import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.messaging.MessagingService;
import de.verdox.vpipeline.api.messaging.builder.MessagingServiceBuilder;
import de.verdox.vpipeline.api.messaging.Transmitter;
import de.verdox.vpipeline.api.messaging.parts.transmitter.RedisTransmitter;
import de.verdox.vpipeline.impl.messaging.MessagingServiceImpl;
import de.verdox.vpipeline.impl.util.RedisConnection;
import org.jetbrains.annotations.NotNull;

public class MessagingServiceBuilderImpl implements MessagingServiceBuilder {

    private Transmitter transmitter;
    private String identifier;


    @Override
    public MessagingServiceBuilder withIdentifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    @Override
    public MessagingServiceBuilder withTransmitter(Transmitter transmitter) {
        this.transmitter = transmitter;
        return this;
    }


    @Override
    public MessagingService buildMessagingService() {
        if (identifier == null)
            throw new NullPointerException("Identifier was not set during building phase.");
        if (transmitter == null)
            throw new NullPointerException("Transmitter was not set during building phase.");
        MessagingServiceImpl messagingService = new MessagingServiceImpl(identifier, transmitter);
        NetworkLogger.info("Building messaging service");
        return messagingService;
    }
}
