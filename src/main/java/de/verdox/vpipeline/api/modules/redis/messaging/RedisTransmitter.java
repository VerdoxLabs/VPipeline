package de.verdox.vpipeline.api.modules.redis.messaging;

import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.messaging.MessagingService;
import de.verdox.vpipeline.api.messaging.Transmitter;
import de.verdox.vpipeline.api.messaging.message.Message;
import de.verdox.vpipeline.impl.util.RedisConnection;
import org.jetbrains.annotations.NotNull;
import org.redisson.api.RTopic;
import org.redisson.api.listener.MessageListener;
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
        globalMessagingChannel = redissonClient.getTopic("GlobalMessagingChannel", new SerializationCodec());

        this.listener = (channel, msg) -> {
            if (messagingService.getSessionUUID().equals(msg.getSender()))
                return;
            NetworkLogger
                    .getLogger()
                    .info("[" + messagingService.getSessionIdentifier() + "] received a message on " + channel);
            messagingService.postMessageEvent(String.valueOf(channel), msg);
        };
        globalMessagingChannel.addListener(Message.class, listener);
    }

    @Override
    public void sendMessage(Message message, UUID... receivers) {
        if (receivers == null || receivers.length == 0) {
            NetworkLogger
                    .getLogger()
                    .info("[" + messagingService.getSessionIdentifier() + "] dumped message because it has no receiver ");
            return;
        }
        for (UUID receiver : receivers) {
            if (receiver.equals(messagingService.getSessionUUID()))
                continue;
            NetworkLogger
                    .getLogger()
                    .info("[" + messagingService.getSessionIdentifier() + "] Sending message to " + receiver);
            getPrivateMessagingChannel(receiver).publish(message);
        }
    }

    @Override
    public void broadcastMessage(Message message) {
        NetworkLogger
                .getLogger()
                .info("[" + messagingService.getSessionIdentifier() + "] Broadcasting message");
        globalMessagingChannel.publish(message);
    }

    @Override
    public void setMessagingService(MessagingService messagingService) {
        if (this.messagingService != null)
            throw new IllegalStateException("MessagingService can't be changed afterwards");
        this.messagingService = messagingService;

        privateMessagingChannel = getPrivateMessagingChannel(messagingService.getSessionUUID());
        privateMessagingChannel.addListener(Message.class, listener);
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
}
