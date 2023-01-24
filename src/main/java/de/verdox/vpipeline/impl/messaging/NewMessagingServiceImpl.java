package de.verdox.vpipeline.impl.messaging;

import com.google.common.eventbus.EventBus;
import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.NetworkParticipant;
import de.verdox.vpipeline.api.messaging.MessageFactory;
import de.verdox.vpipeline.api.messaging.MessagingService;
import de.verdox.vpipeline.api.messaging.RemoteMessageReceiver;
import de.verdox.vpipeline.api.messaging.Transmitter;
import de.verdox.vpipeline.api.messaging.instruction.Instruction;
import de.verdox.vpipeline.api.messaging.instruction.TransmittedData;
import de.verdox.vpipeline.api.messaging.instruction.types.Ping;
import de.verdox.vpipeline.api.messaging.instruction.types.Response;
import de.verdox.vpipeline.api.messaging.message.Message;
import de.verdox.vpipeline.api.network.RemoteParticipant;
import de.verdox.vpipeline.impl.messaging.event.MessageEventImpl;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NewMessagingServiceImpl implements MessagingService {

    private final ConcurrentHashMap<UUID, Instruction<?>> pendingInstructions = new ConcurrentHashMap<>();
    private final Map<UUID, RemoteMessageReceiverImpl> remoteParticipants = new ConcurrentHashMap<>();
    private final Set<RemoteMessageReceiverImpl> receivedKeepAlivePings = ConcurrentHashMap.newKeySet();
    private final EventBus eventBus;
    private final MessageFactory messageFactory;
    private final Transmitter transmitter;
    private final UUID sessionUUID;
    private final String sessionIdentifier;
    private NetworkParticipant networkParticipant;
    private final ScheduledExecutorService keepAliveThread = Executors.newSingleThreadScheduledExecutor();

    public NewMessagingServiceImpl(String sessionIdentifier, Transmitter transmitter) {
        Objects.requireNonNull(sessionIdentifier);
        Objects.requireNonNull(transmitter);
        this.sessionIdentifier = sessionIdentifier;
        this.transmitter = transmitter;
        sessionUUID = RemoteParticipant.getParticipantUUID(sessionIdentifier);
        this.transmitter.setMessagingService(this);
        this.eventBus = new EventBus();
        eventBus.register(this);
        this.messageFactory = new MessageFactoryImpl(this);

        messageFactory.registerInstructionType(9992, OfflinePing.class, () -> new OfflinePing(UUID.randomUUID()));
        messageFactory.registerInstructionType(9993, KeepAlivePing.class, () -> new KeepAlivePing(UUID.randomUUID()));

        remoteParticipants.put(sessionUUID, new RemoteMessageReceiverImpl(sessionUUID, sessionIdentifier));
        sendKeepAlivePing();

        keepAliveThread.scheduleAtFixedRate(() -> {
            /*            sendKeepAlivePing();*/
            remoteParticipants
                    .entrySet()
                    .removeIf(entry -> !receivedKeepAlivePings.contains(entry.getValue()) && !entry
                            .getValue()
                            .getUuid()
                            .equals(sessionUUID));
            receivedKeepAlivePings.clear();
            pendingInstructions.forEach((uuid, instruction) -> pendingInstructions.computeIfPresent(uuid, (uuid1, instruction1) -> {
                if (!instruction1.getResponse().hasReceivedAllAnswers())
                    return instruction1;
                instruction1.getResponse().cancel();
                return null;
            }));
        }, 0, 10, TimeUnit.SECONDS);
    }

    @Override
    public <T> Response<T> sendInstruction(@NotNull Instruction<T> instruction, UUID... receivers) {
        return null;
    }
    @Override
    public Transmitter getTransmitter() {
        return transmitter;
    }

    @Override
    public NetworkParticipant getNetworkParticipant() {
        return networkParticipant;
    }
    @Override
    public MessageFactory getMessageFactory() {
        return messageFactory;
    }

    @Override
    public UUID getSessionUUID() {
        return sessionUUID;
    }

    @Override
    public void shutdown() {
        NetworkLogger.info("Shutting down message transmitter");
        sendOfflinePing();
        this.pendingInstructions.forEach((uuid, instruction) -> instruction.getResponse().cancel());
        this.keepAliveThread.shutdownNow();
        transmitter.shutdown();
        NetworkLogger.info("MessagingService is offline");
    }

    @Override
    public String getSessionIdentifier() {
        return sessionIdentifier;
    }

    @Override
    public Set<RemoteMessageReceiver> getRemoteMessageReceivers() {
        return new HashSet<>(remoteParticipants.values());
    }

    @Override
    public void sendKeepAlivePing() {

    }

    @Override
    public void postMessageEvent(String channelName, Message message) {
        eventBus.post(new MessageEventImpl(channelName, message));
    }

    private void sendOfflinePing() {
        sendInstruction(new OfflinePing(UUID.randomUUID()).withData(sessionUUID, sessionIdentifier));
    }

    public class OfflinePing extends Ping {
        public OfflinePing(@NotNull UUID uuid) {
            super(uuid);
        }

        @Override
        public List<Class<?>> instructionDataTypes() {
            return List.of(UUID.class, String.class);
        }

        @Override
        public void onPingReceive(TransmittedData instructionData) {
            var transmitterUUID = instructionData.transmitter();
            var transmitterID = instructionData.transmitterIdentifier();

            NetworkLogger.debug(transmitterID + " offline on messaging network");
            remoteParticipants.remove(transmitterUUID);
        }
    }

    public class KeepAlivePing extends Ping {

        public KeepAlivePing(@NotNull UUID uuid) {
            super(uuid);
        }

        @Override
        public List<Class<?>> instructionDataTypes() {
            return List.of(UUID.class, String.class);
        }

        @Override
        public void onPingReceive(TransmittedData instructionData) {
            var transmitterUUID = instructionData.transmitter();
            var transmitterID = instructionData.transmitterIdentifier();

            var foundEntry = remoteParticipants.getOrDefault(transmitterUUID, null);
            if (foundEntry == null) {
                NetworkLogger.debug(transmitterID + " online on messaging network");
                remoteParticipants.put(transmitterUUID, new RemoteMessageReceiverImpl(transmitterUUID, transmitterID));
                return;
            }

            NetworkLogger.debug(transmitterID + " pinged alive on messaging network");
            foundEntry.updateKeepAlive();
            receivedKeepAlivePings.add(foundEntry);
        }
    }
}
