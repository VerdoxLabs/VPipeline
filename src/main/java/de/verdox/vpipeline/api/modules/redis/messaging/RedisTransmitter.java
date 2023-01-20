package de.verdox.vpipeline.api.modules.redis.messaging;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.messaging.MessagingService;
import de.verdox.vpipeline.api.messaging.Transmitter;
import de.verdox.vpipeline.api.messaging.message.Message;
import de.verdox.vpipeline.impl.util.RedisConnection;
import org.jetbrains.annotations.NotNull;
import org.redisson.api.RTopic;
import org.redisson.api.listener.MessageListener;
import org.redisson.client.codec.StringCodec;
import org.redisson.codec.SerializationCodec;

import java.util.UUID;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 19.06.2022 12:44
 */
public class RedisTransmitter extends RedisConnection implements Transmitter {

    private final RTopic globalMessagingChannel;
    private final MessageListener<Message> listener;
    private MessagingService messagingService;
    private RTopic privateMessagingChannel;

    public RedisTransmitter(boolean clusterMode, @NotNull String[] addressArray, String redisPassword) {
        super(clusterMode, addressArray, redisPassword);
        //TODO: Don't use Serialization codec but use gson
        globalMessagingChannel = redissonClient.getTopic("GlobalMessagingChannel", new SerializationCodec());
        this.listener = (channel, msg) -> {
            NetworkLogger.fine("[" + messagingService.getSessionIdentifier() + "] received a message on " + channel);
            try {
                messagingService.postMessageEvent(String.valueOf(channel), msg);
            } catch (Throwable e) {
                e.printStackTrace();
            }

        };
        globalMessagingChannel.addListener(Message.class, listener);
        NetworkLogger.info("Redis Transmitter connected");
    }

    @Override
    public long sendMessage(Message message, UUID... receivers) {
        if (receivers == null || receivers.length == 0) {
            NetworkLogger
                    .getLogger()
                    .warning("[" + messagingService.getSessionIdentifier() + "] dumped message because it has no receiver ");
            return 0;
        }
        var counter = 0;
        for (UUID receiver : receivers) {
            if (receiver.equals(messagingService.getSessionUUID())) {
                NetworkLogger.fine("[" + messagingService.getSessionIdentifier() + "] Skipping sending to itself");
                continue;
            }
            NetworkLogger.fine("[" + messagingService.getSessionIdentifier() + "] Sending message to " + receiver);
            counter += publish(getPrivateMessagingChannel(receiver), message);

        }
        return counter;
    }

    @Override
    public long broadcastMessage(Message message) {
        return publish(globalMessagingChannel, message);
    }

    @Override
    public void setMessagingService(MessagingService messagingService) {
        if (this.messagingService != null)
            throw new IllegalStateException("MessagingService can't be changed afterwards");
        this.messagingService = messagingService;

        privateMessagingChannel = getPrivateMessagingChannel(messagingService.getSessionUUID());
        privateMessagingChannel.addListener(Message.class, listener);

        NetworkLogger.info("Private Channel: " + "PrivateMessagingChannel_" + messagingService.getSessionUUID());
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
        return redissonClient.getTopic("PrivateMessagingChannel_" + uuid, new SerializationCodec());
    }

    private long publish(RTopic rTopic, Message message) {
        return rTopic.publish(message);
    }
}
