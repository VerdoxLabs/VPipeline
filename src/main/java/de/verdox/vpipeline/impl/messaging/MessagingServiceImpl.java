package de.verdox.vpipeline.impl.messaging;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.NetworkParticipant;
import de.verdox.vpipeline.api.messaging.MessageFactory;
import de.verdox.vpipeline.api.messaging.MessagingService;
import de.verdox.vpipeline.api.messaging.RemoteMessageReceiver;
import de.verdox.vpipeline.api.messaging.Transmitter;
import de.verdox.vpipeline.api.messaging.annotations.InstructionInfo;
import de.verdox.vpipeline.api.messaging.event.MessageEvent;
import de.verdox.vpipeline.api.messaging.instruction.AbstractInstruction;
import de.verdox.vpipeline.api.messaging.instruction.Instruction;
import de.verdox.vpipeline.api.messaging.instruction.ResponseCollector;
import de.verdox.vpipeline.api.messaging.instruction.types.Ping;
import de.verdox.vpipeline.api.network.RemoteParticipant;
import de.verdox.vpipeline.impl.messaging.event.MessageEventImpl;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class MessagingServiceImpl implements MessagingService {

    private final ConcurrentHashMap<UUID, Instruction<?>> pendingInstructions = new ConcurrentHashMap<>();
    private final Map<UUID, RemoteMessageReceiverImpl> remoteParticipants = new ConcurrentHashMap<>();
    private final Set<RemoteMessageReceiverImpl> receivedKeepAlivePings = ConcurrentHashMap.newKeySet();
    private final EventBus eventBus;
    private MessageFactoryImpl messageFactoryImpl;
    private final Transmitter transmitter;
    private final UUID sessionUUID;
    private final String sessionIdentifier;
    private NetworkParticipant networkParticipant;
    private final ScheduledExecutorService keepAliveThread = Executors.newSingleThreadScheduledExecutor();

    public MessagingServiceImpl(String sessionIdentifier, Transmitter transmitter) {
        Objects.requireNonNull(sessionIdentifier);
        Objects.requireNonNull(transmitter);
        this.sessionIdentifier = sessionIdentifier;
        this.transmitter = transmitter;
        sessionUUID = RemoteParticipant.getParticipantUUID(sessionIdentifier);

        this.messageFactoryImpl = new MessageFactoryImpl(this);
        this.messageFactoryImpl.registerInstructionType(9998, KeepAlivePing.class, () -> new KeepAlivePing(UUID.randomUUID()));
        this.messageFactoryImpl.registerInstructionType(9999, OfflinePing.class, () -> new OfflinePing(UUID.randomUUID()));
        this.transmitter.setMessagingService(this);

        this.eventBus = new EventBus();
        eventBus.register(this);

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
                if (!instruction1.getResponseCollector().hasReceivedAllAnswers())
                    return instruction1;
                if (instruction1.getResponseCollector() instanceof ResponseCollectorImpl<?> responseCollector)
                    responseCollector.cancel();
                return null;
            }));
        }, 0, 10, TimeUnit.SECONDS);
    }

    @Subscribe
    private void onMessage(MessageEvent messageEvent) {
        try {
            var instruction = messageEvent.getMessage();
            if (instruction == null) {
                throw new IllegalArgumentException("[" + getSessionIdentifier() + "] Instruction in event was null");
            }
            if (!(instruction instanceof AbstractInstruction<?> abstractInstruction)) {
                NetworkLogger.warning("[" + getSessionIdentifier() + "] Message of type " + instruction.getClass()
                                                                                                       .getSimpleName() + " in event is not a subtype of " + AbstractInstruction.class.getSimpleName());
                return;
            }


            if (!messageFactoryImpl.isTypeRegistered((Class<? extends Instruction<?>>) instruction.getClass())) {
                NetworkLogger.warning("[" + getSessionIdentifier() + "] Message type is unknown");
                return;
            }

            if (!instruction.isResponse())
                handleInstruction(abstractInstruction);
            else
                handleResponse(abstractInstruction);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public <R, T extends Instruction<R>> ResponseCollector<R> sendInstruction(@NotNull T instruction, UUID... receivers) {
        if (!(instruction instanceof AbstractInstruction<?> abstractInstruction)) {
            NetworkLogger.warning("[" + getSessionIdentifier() + "] Message is not a subtype of " + AbstractInstruction.class.getSimpleName());
            return null;
        }

        var receiversAmount = receivers.length == 0 ? transmitter.getNetworkTransmitterAmount() : receivers.length;
        InstructionInfo instructionInfo = messageFactoryImpl.findInstructionInfo((Class<? extends AbstractInstruction<?>>) instruction.getClass());
        var registeredID = messageFactoryImpl.findInstructionID(instruction);
        if (registeredID == -1)
            throw new IllegalArgumentException("[" + getSessionIdentifier() + "] Instruction of type " + instruction
                    .getClass()
                    .getSimpleName() + " is not registered yet");

        NetworkLogger.debug("[" + getSessionIdentifier() + "] Sending instruction " + instruction.getClass()
                                                                                                 .getSimpleName());

        if (instruction.onSend(this, receiversAmount)) {
            abstractInstruction.setupInstruction(registeredID, getSessionUUID(), getSessionIdentifier());
            abstractInstruction.setResponseCollector(new ResponseCollectorImpl<>(receiversAmount));
            if (receivers.length == 0) {
                transmitter.broadcastMessage(instruction);
            } else {
                transmitter.sendMessage(instruction, receivers);
            }
            if (instructionInfo.awaitsResponse())
                pendingInstructions.put(instruction.getUuid(), instruction);
        } else {
            abstractInstruction.setResponseCollector(new ResponseCollectorImpl<>(0));
            var responseCollector = (ResponseCollectorImpl<R>) abstractInstruction.getResponseCollector();

            if (abstractInstruction.getResponseToSend() != null) {
                NetworkLogger.debug("[" + getSessionIdentifier() + "] Instruction was executed locally.");
                responseCollector.complete(getSessionUUID(), (R) abstractInstruction.getResponseToSend());

            } else {
                NetworkLogger.debug("[" + getSessionIdentifier() + "] Instruction was cancelled before sending");
                responseCollector.cancel();
            }

        }
        return instruction.getResponseCollector();
    }

    @Override
    public <R, T extends Instruction<R>> ResponseCollector<R> sendInstruction(@NotNull Class<? extends T> instructionType, Consumer<T> consumer, UUID... receivers) {

        var id = getMessageFactory().findInstructionID(instructionType);
        if (id == -1)
            throw new IllegalArgumentException("[" + getSessionIdentifier() + "] Instruction not registered in message factory yet");


        var instruction = getMessageFactory().getInstructionType(id).instanceSupplier().get();
        consumer.accept((T) instruction);

        return (ResponseCollector<R>) sendInstruction(instruction, receivers);
    }

    private <R, T extends AbstractInstruction<R>> void handleInstruction(@NotNull T instruction) {
        var response = instruction.onInstructionReceive(this);
        var instructionInfo = messageFactoryImpl.findInstructionInfo((Class<? extends AbstractInstruction<?>>) instruction.getClass());
        NetworkLogger.debug("[" + getSessionIdentifier() + "] Received instruction of type: " + instruction.getClass()
                                                                                                          .getSimpleName());
        if (!instructionInfo.awaitsResponse()) {
            NetworkLogger.debug("[" + getSessionIdentifier() + "] " + instruction.getClass()
                                                                                 .getSimpleName() + " does not await a response");
            return;
        }


        sendResponse(instruction, response);
    }

    private <R, T extends AbstractInstruction<R>> void sendResponse(@NotNull T instruction, R response) {
        NetworkLogger.debug("[" + getSessionIdentifier() + "] Sending response for " + instruction.getClass()
                                                                                                 .getSimpleName() + " to " + instruction.getSenderIdentifier());
        instruction.setResponseToSend(response);

        var originalSender = instruction.getSenderUUID();

        instruction.setupInstruction(instruction.getInstructionID(), getSessionUUID(), getSessionIdentifier());

        getTransmitter().sendMessage(instruction, originalSender);
    }

    private <R, T extends AbstractInstruction<R>> void handleResponse(@NotNull T response) {
        NetworkLogger.debug("[" + getSessionIdentifier() + "] Received response from" + response
                .getSenderIdentifier());
        if (!pendingInstructions.containsKey(response.getUuid()))
            return;
        T instructionLeft = (T) pendingInstructions.get(response.getUuid());
        NetworkLogger.debug("Handling response");
        if (response.isResponse() && !instructionLeft.isResponse()) {
            if (instructionLeft.getResponseCollector() instanceof ResponseCollectorImpl responseCollector)
                responseCollector.complete(response.getSenderUUID(), response.getResponseToSend());
            instructionLeft.onResponseReceive(this, response.getResponseToSend());
        }
    }


    public void postMessageEvent(String channelName, Instruction<?> message) {
        Objects.requireNonNull(channelName);
        Objects.requireNonNull(message);
        this.eventBus.post(new MessageEventImpl(channelName, message));
    }

    public void setNetworkParticipant(NetworkParticipant networkParticipant) {
        this.networkParticipant = networkParticipant;
    }

    @Override
    public Set<RemoteMessageReceiver> getRemoteMessageReceivers() {
        return new HashSet<>(remoteParticipants.values());
    }

    @Override
    public void sendKeepAlivePing() {
        sendInstruction(KeepAlivePing.class, keepAlivePing -> {
        });
    }

    private void sendOfflinePing() {
        sendInstruction(OfflinePing.class, offlinePing -> {
        });
    }

    @Override
    public void shutdown() {
        NetworkLogger.info("Shutting down message transmitter");
        sendOfflinePing();
        this.pendingInstructions.forEach((uuid, instruction) -> {
            if (instruction.getResponseCollector() instanceof ResponseCollectorImpl<?> responseCollector)
                responseCollector.cancel();
        });
        this.keepAliveThread.shutdownNow();
        transmitter.shutdown();
        NetworkLogger.info("MessagingService is offline");
    }

    @Override
    public NetworkParticipant getNetworkParticipant() {
        return networkParticipant;
    }

    @Override
    public Transmitter getTransmitter() {
        return transmitter;
    }

    @Override
    public UUID getSessionUUID() {
        return sessionUUID;
    }

    @Override
    public String getSessionIdentifier() {
        return sessionIdentifier;
    }

    @Override
    public MessageFactory getMessageFactory() {
        return messageFactoryImpl;
    }

    public class KeepAlivePing extends Ping {
        public KeepAlivePing(@NotNull UUID uuid) {
            super(uuid);
        }

        @Override
        public void onPingReceive(MessagingService messagingService) {
            if (messagingService instanceof MessagingServiceImpl messagingServiceImpl)
                messagingServiceImpl.receivedKeepAlivePings.add(new RemoteMessageReceiverImpl(this.getSenderUUID(), this.getSenderIdentifier()));
        }
    }

    public class OfflinePing extends Ping {
        public OfflinePing(@NotNull UUID uuid) {
            super(uuid);
        }

        @Override
        public void onPingReceive(MessagingService messagingService) {
            var sender = getSenderUUID();

            if (messagingService instanceof MessagingServiceImpl messagingServiceImpl)
                messagingServiceImpl.remoteParticipants.remove(sender);
        }
    }
}
