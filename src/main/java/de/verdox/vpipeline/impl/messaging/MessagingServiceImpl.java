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
import de.verdox.vpipeline.api.messaging.instruction.Instruction;
import de.verdox.vpipeline.api.messaging.instruction.Responder;
import de.verdox.vpipeline.api.messaging.instruction.SimpleInstruction;
import de.verdox.vpipeline.api.messaging.instruction.TransmittedData;
import de.verdox.vpipeline.api.messaging.instruction.types.Ping;
import de.verdox.vpipeline.api.messaging.instruction.types.Response;
import de.verdox.vpipeline.api.messaging.message.Message;
import de.verdox.vpipeline.api.messaging.message.MessageWrapper;
import de.verdox.vpipeline.api.network.RemoteParticipant;
import de.verdox.vpipeline.impl.messaging.event.MessageEventImpl;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 23:21
 */
public class MessagingServiceImpl implements MessagingService {
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

    public MessagingServiceImpl(String sessionIdentifier, Transmitter transmitter) {
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

    public void setNetworkParticipant(NetworkParticipant networkParticipant) {
        this.networkParticipant = networkParticipant;
    }

    @Override
    public NetworkParticipant getNetworkParticipant() {
        return networkParticipant;
    }

    @Override
    public <T> Response<T> sendInstruction(@NotNull Instruction<T> instruction) {
        return sendInstruction(instruction, new UUID[0]);
    }

    @Override
    public <T> Response<T> sendInstruction(@NotNull Instruction<T> instruction, UUID... receivers) {
        if (!(instruction instanceof SimpleInstruction<?> instructionImpl)) {
            NetworkLogger.fine("[" + sessionIdentifier + "] Dumping unknown instruction");
            return instruction.getResponse();
        }
        NetworkLogger.fine("[" + sessionIdentifier + "] Sending instruction");
        instructionImpl.setNetworkParticipant(networkParticipant);

        InstructionInfo instructionInfo = getMessageFactory().findInstructionInfo((Class<? extends Instruction<?>>) instruction.getClass());
        UUID uuid = instruction.getUUID();

        Message message = getMessageFactory().constructMessage(instruction);
        Objects.requireNonNull(message);

        var receiversAmount = receivers.length == 0 ? transmitter.getNetworkTransmitterAmount() : receivers.length;

        if (instruction.onSend(new TransmittedData(sessionUUID, getSessionIdentifier(), instruction.getData()), receiversAmount)) {
            if (receivers.length == 0)
                getTransmitter().broadcastMessage(message);
            else
                getTransmitter().sendMessage(message, receivers);
            if (instructionInfo.awaitsResponse())
                pendingInstructions.put(uuid, instruction);
        }
        return instruction.getResponse();
    }

    @Override
    public Transmitter getTransmitter() {
        return transmitter;
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
    public String getSessionIdentifier() {
        return sessionIdentifier;
    }

    @Override
    public boolean isOwnMessage(Message message) {
        return new MessageWrapper(message).getSenderUUID().equals(sessionUUID);
    }

    @Override
    public void postMessageEvent(String channelName, Message message) {
        eventBus.post(new MessageEventImpl(channelName, message));
    }

    @Override
    public Set<RemoteMessageReceiver> getRemoteMessageReceivers() {
        return new HashSet<>(remoteParticipants.values());
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

    @Subscribe
    private void onMessage(MessageEvent messageEvent) {
        try {
            MessageWrapper messageWrapper = new MessageWrapper(messageEvent.getMessage());
            if (!isValidMessage(messageWrapper))
                return;

            var cachedInstructionData = getMessageFactory().getInstructionType(messageWrapper.getInstructionID());
            if (cachedInstructionData == null)
                return;
            var instructionType = cachedInstructionData.type();

            if (messageWrapper.isInstruction()) {
                NetworkLogger.fine("Message is a " + instructionType.getSimpleName() + " instruction");
                handleInstruction(messageWrapper, cachedInstructionData);
            } else if (messageWrapper.isResponse()) {
                NetworkLogger.fine("Message is a " + instructionType.getSimpleName() + " response");
                handleResponse(messageWrapper);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleInstruction(MessageWrapper messageWrapper, MessageFactory.CachedInstructionData<?> instructionData) {
        InstructionInfo instructionInfo = getMessageFactory().findInstructionInfo(instructionData.type());
        Instruction<?> response = instructionData.instanceSupplier().get();
        var instructionType = instructionData.type();
        response.onReceive(new TransmittedData(messageWrapper.getSenderUUID(), messageWrapper.getSenderIdentifier(), messageWrapper.getData()));
        if (!instructionInfo.awaitsResponse()) {
            NetworkLogger.fine(instructionType.getSimpleName() + " does not await a response");
            return;
        }

        if (!(response instanceof SimpleInstruction<?> instructionImpl) || !(response instanceof Responder responder) || (pendingInstructions.containsKey(messageWrapper.getSenderUUID()) && !responder.respondToItself()))
            return;
        instructionImpl.setNetworkParticipant(networkParticipant);
        NetworkLogger.fine("[" + getSessionIdentifier() + "] Answering " + instructionType.getSimpleName());
        Object[] responseData = ((Responder) response).respondToData(new TransmittedData(messageWrapper.getSenderUUID(), messageWrapper.getSenderIdentifier(), messageWrapper.getData()));
        if (responseData == null || responseData.length == 0) {
            NetworkLogger.fine("[" + getSessionIdentifier() + "] " + instructionType.getSimpleName() + " response is empty so it wont be sent");
            return;
        }
        sendResponse(messageWrapper, responseData);
    }

    private void handleResponse(MessageWrapper messageWrapper) {
        if (!pendingInstructions.containsKey(messageWrapper.getInstructionUUID()))
            return;
        Instruction<?> instructionLeft = pendingInstructions.get(messageWrapper.getInstructionUUID());
        if (!(instructionLeft instanceof SimpleInstruction<?> instructionImpl) || !(instructionLeft instanceof Responder responder))
            return;
        instructionImpl.setNetworkParticipant(networkParticipant);
        responder.onResponseReceive(new TransmittedData(getSessionUUID(), getSessionIdentifier(), messageWrapper.getData()), new TransmittedData(messageWrapper.getSenderUUID(), messageWrapper.getSenderIdentifier(), messageWrapper.getResponseData()));
    }

    private void sendResponse(MessageWrapper messageWrapper, Object[] responseData) {
        Message response = getMessageFactory().constructResponse(messageWrapper.getInstructionID(), messageWrapper.getInstructionUUID(), messageWrapper.getParameters(), messageWrapper.getData(), responseData);
        NetworkLogger.fine("[" + getSessionIdentifier() + "] Sending response to " + messageWrapper
                .message()
                .getSenderIdentifier());
        getTransmitter().sendMessage(response, messageWrapper.getSenderUUID());
    }

    private boolean isValidMessage(MessageWrapper wrapper) {
        return wrapper.parameterContains(INSTRUCTION_IDENTIFIER) || wrapper.parameterContains(RESPONSE_IDENTIFIER);
    }

    private void sendOfflinePing() {
        sendInstruction(new OfflinePing(UUID.randomUUID()).withData(sessionUUID, sessionIdentifier));
    }

    @Override
    public void sendKeepAlivePing() {
        sendInstruction(new KeepAlivePing(UUID.randomUUID()).withData(sessionUUID, sessionIdentifier));
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
            var transmitterUUID = instructionData.getObject(0, UUID.class);
            var transmitterID = instructionData.getObject(1, String.class);

            NetworkLogger.fine(transmitterID + " offline on messaging network");
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
            var transmitterUUID = instructionData.getObject(0, UUID.class);
            var transmitterID = instructionData.getObject(1, String.class);

            var foundEntry = remoteParticipants.getOrDefault(transmitterUUID, null);
            if (foundEntry == null) {
                NetworkLogger.fine(transmitterID + " online on messaging network");
                remoteParticipants.put(transmitterUUID, new RemoteMessageReceiverImpl(transmitterUUID, transmitterID));
                return;
            }

            NetworkLogger.fine(transmitterID + " pinged alive on messaging network");
            foundEntry.updateKeepAlive();
            receivedKeepAlivePings.add(foundEntry);
        }
    }
}
