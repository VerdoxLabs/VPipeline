package de.verdox.vpipeline.api.messaging;

import org.jetbrains.annotations.NotNull;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 23.06.2022 11:44
 */
public interface IMessagingServiceBuilder {
    IMessagingServiceBuilder withIdentifier(String identifier);

    IMessagingServiceBuilder useRedisTransmitter(boolean clusterMode, @NotNull String[] addressArray, String redisPassword);

    IMessagingService buildMessagingService();
}
