package de.verdox.vpipeline.impl.messaging.builder;

import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.messaging.MessagingService;
import de.verdox.vpipeline.api.messaging.builder.MessagingServiceBuilder;
import de.verdox.vpipeline.api.messaging.Transmitter;
import de.verdox.vpipeline.api.modules.redis.messaging.RedisTransmitter;
import de.verdox.vpipeline.impl.messaging.MessagingServiceImpl;
import org.jetbrains.annotations.NotNull;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 23.06.2022 11:46
 */
public class MessagingServiceBuilderImpl implements MessagingServiceBuilder {

    private Transmitter transmitter;
    private String identifier;


    @Override
    public MessagingServiceBuilder withIdentifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    @Override
    public MessagingServiceBuilder useRedisTransmitter(boolean clusterMode, @NotNull String[] addressArray, String redisPassword) {
        this.transmitter = new RedisTransmitter(clusterMode, addressArray, redisPassword);
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
