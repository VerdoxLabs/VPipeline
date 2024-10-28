package de.verdox.vpipeline.api.messaging;

import de.verdox.vpipeline.api.Connection;
import de.verdox.vpipeline.api.messaging.instruction.Instruction;
import de.verdox.vpipeline.api.messaging.parts.transmitter.DummyTransmitter;
import de.verdox.vpipeline.api.messaging.parts.transmitter.RedisTransmitter;
import de.verdox.vpipeline.api.pipeline.core.SystemPart;
import de.verdox.vpipeline.impl.util.RedisConnection;
import de.verdox.vserializer.generic.Serializer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Sends instruction messages to other network participants
 */
public interface Transmitter extends SystemPart, Connection {

    Serializer<Transmitter> SERIALIZER = Serializer.Selection.create("transmitter", Transmitter.class)
            .variant("dummy", Serializer.Dummy.create(new DummyTransmitter()))
            .variant("redis", RedisTransmitter.SERIALIZER, new RedisTransmitter(new RedisConnection(false, new String[]{"redis://localhost:6379"}, "")))
            ;

    long sendMessage(Instruction<?> message, UUID... receivers);

    long broadcastMessage(Instruction<?> message);

    void setMessagingService(MessagingService messagingService);

    long getNetworkTransmitterAmount();

    static Transmitter createRedisTransmitter(boolean clusterMode, @NotNull String[] addressArray, String redisPassword) {
        return createRedisTransmitter(new RedisConnection(clusterMode, addressArray, redisPassword));
    }

    static Transmitter createRedisTransmitter(RedisConnection redisConnection) {
        return new RedisTransmitter(redisConnection);
    }

    static Transmitter createDummyTransmitter() {
        return new DummyTransmitter();
    }
}
