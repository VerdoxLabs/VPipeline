package de.verdox.vpipeline.api.modules.redis.messaging;

import com.google.gson.*;
import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.messaging.MessagingService;
import de.verdox.vpipeline.api.messaging.Transmitter;
import de.verdox.vpipeline.api.messaging.instruction.Instruction;
import de.verdox.vpipeline.impl.util.RedisConnection;
import org.jetbrains.annotations.NotNull;
import org.redisson.api.RTopic;
import org.redisson.api.listener.MessageListener;
import org.redisson.client.codec.StringCodec;
import org.redisson.codec.SerializationCodec;

import java.util.Objects;
import java.util.UUID;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 19.06.2022 12:44
 */
public class RedisTransmitter extends RedisConnection implements Transmitter {

    private final RTopic globalMessagingChannel;
    private final MessageListener<String> listener;
    private MessagingService messagingService;
    private RTopic privateMessagingChannel;

    private final Gson gson = new GsonBuilder().serializeNulls().create();

    public RedisTransmitter(boolean clusterMode, @NotNull String[] addressArray, String redisPassword) {
        super(clusterMode, addressArray, redisPassword);

        globalMessagingChannel = redissonClient.getTopic("GlobalMessagingChannel", new StringCodec());
        this.listener = (channel, msgString) -> {
            //TODO: Den richtigen Typ hernehmen nach ID zum deserializen
            //TODO: Die Id mitsenden?
            var element = JsonParser.parseString(msgString);


            var instructionID = element.getAsJsonObject().get("id").getAsJsonPrimitive().getAsInt();
            var data = element.getAsJsonObject().get("data");
            var type = messagingService.getMessageFactory().getInstructionType(instructionID);
            if (type == null) {
                NetworkLogger.debug("[" + messagingService.getSessionIdentifier() + "] Received unknown data with id: " + instructionID);
                return;
            }

            var deserializedInstruction = gson.fromJson(data, type.type());
            if (deserializedInstruction.getSenderUUID().equals(messagingService.getSessionUUID()))
                return;
            NetworkLogger.debug("[" + messagingService.getSessionIdentifier() + "] Received a message on global channel");
            NetworkLogger.debug("[" + messagingService.getSessionIdentifier() + "] Deserialized message to " + type
                    .type().getSimpleName());
            //NetworkLogger.info("[" + messagingService.getSessionIdentifier() + "] received a message on " + channel);
            try {
                messagingService.postMessageEvent(String.valueOf(channel), deserializedInstruction);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        };
        globalMessagingChannel.addListener(String.class, listener);
        NetworkLogger.info("Redis Transmitter connected");
    }


    @Override
    public long sendMessage(Instruction<?> message, UUID... receivers) {
        if (receivers == null || receivers.length == 0)
            return broadcastMessage(message);
        var counter = 0;
        for (UUID receiver : receivers) {
            if (receiver.equals(messagingService.getSessionUUID())) {
                NetworkLogger.warning("[" + messagingService.getSessionIdentifier() + "] Skipping sending to itself");
                continue;
            }
            NetworkLogger.debug("[" + messagingService.getSessionIdentifier() + "] Sending message to PrivateMessagingChannel_" + receiver);
            counter += publish(getPrivateMessagingChannel(receiver), message);

        }
        return counter;
    }

    @Override
    public long broadcastMessage(Instruction<?> message) {
        Objects.requireNonNull(message);
        var publishedTo = publish(globalMessagingChannel, message);
        var amountSubscribers = globalMessagingChannel.countSubscribers();
        if (publishedTo != amountSubscribers)
            NetworkLogger.warning("[" + messagingService.getSessionIdentifier() + "] Broadcast message couldn't be sent to all subscribers [" + publishedTo + "/" + amountSubscribers + "] - " + message);
        else
            NetworkLogger.debug("[" + messagingService.getSessionIdentifier() + "] Message was broadcasted to " + publishedTo + "/" + amountSubscribers + " clients.");
        return publishedTo;
    }

    @Override
    public void setMessagingService(MessagingService messagingService) {
        if (this.messagingService != null)
            throw new IllegalStateException("MessagingService can't be changed afterwards");
        this.messagingService = messagingService;

        privateMessagingChannel = getPrivateMessagingChannel(messagingService.getSessionUUID());
        privateMessagingChannel.addListener(String.class, listener);

        NetworkLogger.info("[" + messagingService.getSessionIdentifier() + "] Private Channel: " + "PrivateMessagingChannel_" + messagingService.getSessionUUID());
    }

    @Override
    public long getNetworkTransmitterAmount() {
        return this.globalMessagingChannel.countSubscribers();
    }

    @Override
    public void shutdown() {
        redissonClient.shutdown();
        globalMessagingChannel.removeListener(listener);
        privateMessagingChannel.removeListener(listener);
    }

    private RTopic getPrivateMessagingChannel(UUID uuid) {
        return redissonClient.getTopic("PrivateMessagingChannel_" + uuid, new StringCodec());
    }

    private long publish(RTopic rTopic, Instruction<?> instruction) {
        var object = new JsonObject();
        object.add("id", new JsonPrimitive(instruction.getInstructionID()));
        object.add("data", gson.toJsonTree(instruction));
        return rTopic.publish(gson.toJson(object));
    }
}
