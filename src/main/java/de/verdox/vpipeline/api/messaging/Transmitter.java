package de.verdox.vpipeline.api.messaging;

import de.verdox.vpipeline.api.messaging.instruction.Instruction;
import de.verdox.vpipeline.api.messaging.parts.transmitter.DummyTransmitter;
import de.verdox.vpipeline.api.messaging.parts.transmitter.RedisTransmitter;
import de.verdox.vpipeline.api.pipeline.core.SystemPart;
import de.verdox.vpipeline.api.pipeline.parts.GlobalCache;
import de.verdox.vpipeline.api.pipeline.parts.cache.global.RedisCache;
import de.verdox.vpipeline.impl.util.RedisConnection;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Sends instruction messages to other network participants
 */
public interface Transmitter extends SystemPart {
    long sendMessage(Instruction<?> message, UUID... receivers);

    long broadcastMessage(Instruction<?> message);

    void setMessagingService(MessagingService messagingService);

    long getNetworkTransmitterAmount();

    static Transmitter createRedisTransmitter(boolean clusterMode, @NotNull String[] addressArray, String redisPassword) {
        return new RedisTransmitter(new RedisConnection(clusterMode, addressArray, redisPassword));
    }

    static Transmitter createDummyTransmitter() {
        return new DummyTransmitter();
    }
}
