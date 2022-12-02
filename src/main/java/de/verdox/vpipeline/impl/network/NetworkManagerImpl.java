package de.verdox.vpipeline.impl.network;

import de.verdox.vpipeline.api.NetworkParticipant;
import de.verdox.vpipeline.api.messaging.MessagingService;
import de.verdox.vpipeline.api.network.NetworkManager;
import de.verdox.vpipeline.api.pipeline.core.Pipeline;
import de.verdox.vpipeline.api.pipeline.core.SystemPart;
import de.verdox.vpipeline.impl.network.pings.ClientKeepAlivePing;
import de.verdox.vpipeline.impl.network.pings.ClientOfflinePing;
import de.verdox.vpipeline.impl.network.pings.ClientOnlinePing;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 03.07.2022 23:08
 */
public class NetworkManagerImpl implements SystemPart, NetworkManager {

    private final Pipeline iPipeline;
    private final MessagingService messagingService;
    private final ScheduledFuture<?> keepAlivePing;
    private NetworkParticipant networkParticipant;

    public NetworkManagerImpl(Pipeline iPipeline, MessagingService messagingService) {
        this.iPipeline = iPipeline;
        this.messagingService = messagingService;
        this.keepAlivePing = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::sendKeepAlivePing, 0, 5, TimeUnit.SECONDS);
    }

    public void setNetworkParticipant(NetworkParticipant networkParticipant) {
        this.networkParticipant = networkParticipant;
    }

    private void sendOnlinePing() {
        messagingService.sendInstruction(new ClientOnlinePing(messagingService.getSessionUUID()));
    }

    private void sendOfflinePing() {
        messagingService.sendInstruction(new ClientOfflinePing(messagingService.getSessionUUID()));
    }

    private void sendKeepAlivePing() {
        messagingService.sendInstruction(new ClientKeepAlivePing(messagingService.getSessionUUID()));
    }

    @Override
    public void shutdown() {
        keepAlivePing.cancel(false);
        sendOfflinePing();
    }

    @Override
    public NetworkParticipant getNetworkParticipant() {
        return this.networkParticipant;
    }
}
