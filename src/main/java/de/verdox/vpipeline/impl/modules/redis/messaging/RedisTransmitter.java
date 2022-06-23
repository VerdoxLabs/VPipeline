package de.verdox.vpipeline.impl.modules.redis.messaging;

import de.verdox.vpipeline.api.messaging.IMessagingService;
import de.verdox.vpipeline.api.messaging.ITransmitter;
import de.verdox.vpipeline.api.messaging.message.IMessage;
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
public class RedisTransmitter extends RedisConnection implements ITransmitter {

    private final RTopic globalMessagingChannel;
    private final MessageListener<IMessage> listener;
    private IMessagingService messagingService;

    private RTopic privateMessagingChannel;

    public RedisTransmitter(boolean clusterMode, @NotNull String[] addressArray, String redisPassword) {
        super(clusterMode, addressArray, redisPassword);
        globalMessagingChannel = redissonClient.getTopic("GlobalMessagingChannel", new SerializationCodec());

        this.listener = (channel, msg) -> {
            if (msg == null)
                return;
            if (messagingService.isOwnMessage(msg))
                return;
            messagingService.postMessageEvent(channel.toString(), msg);
        };

        privateMessagingChannel = getPrivateMessagingChannel(messagingService.getSessionUUID());
        privateMessagingChannel.addListener(IMessage.class, listener);
    }

    @Override
    public void sendMessage(IMessage message, UUID... receivers) {
        if (receivers == null || receivers.length == 0)
            return;
        for (UUID receiver : receivers) {
            if (receiver.equals(messagingService.getSessionUUID()))
                continue;
            getPrivateMessagingChannel(receiver).publish(receiver);
        }
    }

    @Override
    public void broadcastMessage(IMessage message) {
        globalMessagingChannel.publish(message);
    }

    @Override
    public void setMessagingService(IMessagingService messagingService) {
        if (this.messagingService != null)
            throw new IllegalStateException("MessagingService can't be changed afterwards");
        this.messagingService = messagingService;
    }

    @Override
    public void shutdown() {
        globalMessagingChannel.removeListener(listener);
        privateMessagingChannel.removeListener(listener);
    }

    private RTopic getPrivateMessagingChannel(UUID uuid) {
        return redissonClient.getTopic("PrivateMessagingChannel_" + messagingService.getSessionUUID(), new SerializationCodec());
    }
}
