package de.verdox.vpipeline.api.messaging.builder;

import de.verdox.vpipeline.api.messaging.MessagingService;
import org.jetbrains.annotations.NotNull;


public interface MessagingServiceBuilder {
    MessagingServiceBuilder withIdentifier(String identifier);

    MessagingServiceBuilder useRedisTransmitter(boolean clusterMode, @NotNull String[] addressArray, String redisPassword);

    MessagingService buildMessagingService();
}
