package de.verdox.vpipeline.api.messaging.builder;

import de.verdox.vpipeline.api.messaging.MessagingService;
import org.jetbrains.annotations.NotNull;


/**
 * Used to create a MessagingService
 */
public interface MessagingServiceBuilder {
    /**
     * Used to specify the identifier of the messaging service
     * @param identifier the identifier
     * @return the builder
     */
    MessagingServiceBuilder withIdentifier(String identifier);

    /**
     * Used to setup a redis transmitter for messaging
     * @param clusterMode whether to use redis cluster mode
     * @param addressArray the addresses of the redis servers
     * @param redisPassword the redis password
     * @return the builder
     */
    MessagingServiceBuilder useRedisTransmitter(boolean clusterMode, @NotNull String[] addressArray, String redisPassword);

    /**
     * Used to build the messaging service
     * @return the built messaging service
     */
    MessagingService buildMessagingService();
}
