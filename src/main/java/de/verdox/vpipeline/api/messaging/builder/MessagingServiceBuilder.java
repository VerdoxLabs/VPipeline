package de.verdox.vpipeline.api.messaging.builder;

import de.verdox.vpipeline.api.messaging.MessagingService;
import de.verdox.vpipeline.api.messaging.Transmitter;
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
     * Used to specify the message transmitter to be used
     * @param transmitter the transmitter
     * @return the builder
     */
    MessagingServiceBuilder withTransmitter(Transmitter transmitter);

    /**
     * Used to build the messaging service
     * @return the built messaging service
     */
    MessagingService buildMessagingService();
}
