package de.verdox.vpipeline.impl.messaging;

import de.verdox.vpipeline.api.messaging.IMessagingService;
import de.verdox.vpipeline.api.messaging.IMessagingServiceBuilder;
import de.verdox.vpipeline.api.messaging.ITransmitter;
import de.verdox.vpipeline.impl.modules.redis.messaging.RedisTransmitter;
import org.jetbrains.annotations.NotNull;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 23.06.2022 11:46
 */
public class MessagingServiceBuilder implements IMessagingServiceBuilder {

    private ITransmitter transmitter;
    private String identifier;


    @Override
    public IMessagingServiceBuilder withIdentifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    @Override
    public IMessagingServiceBuilder useRedisTransmitter(boolean clusterMode, @NotNull String[] addressArray, String redisPassword) {
        this.transmitter = new RedisTransmitter(clusterMode, addressArray, redisPassword);
        return this;
    }

    @Override
    public IMessagingService buildMessagingService() {
        if (identifier == null)
            throw new NullPointerException("Identifier was not set during building phase.");
        if (transmitter == null)
            throw new NullPointerException("Transmitter was not set during building phase.");
        return new MessagingService(identifier, transmitter);
    }
}
