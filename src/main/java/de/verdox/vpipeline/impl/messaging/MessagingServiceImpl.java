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
    public <T> Response<T> sendInstruction(@NotNull Instruction<T> instruction, UUID... receivers) {
        if (!(instruction instanceof SimpleInstruction<?> instructionImpl)) {
            NetworkLogger.warning("[" + sessionIdentifier + "] Dumping unknown instruction");
            return instruction.getResponse();
        }
        NetworkLogger.debug("[" + sessionIdentifier + "] Sending instruction " + instruction.getClass()
                                                                                            .getSimpleName());
        instructionImpl.setNetworkParticipant(networkParticipant);

        InstructionInfo instructionInfo = getMessageFactory().findInstructionInfo((Class<? extends Instruction<?>>) instruction.getClass());
        UUID uuid = instruction.getUUID();

        Message message = getMessageFactory().constructMessage(instruction);
        Objects.requireNonNull(message);

        var receiversAmount = receivers.length == 0 ? transmitter.getNetworkTransmitterAmount() : receivers.length;

        if (instruction.onSend(new TransmittedData(sessionUUID, getSessionIdentifier(), List.of(instruction.getData())), receiversAmount)) {
            if (receivers.length == 0) {
                if (!(instruction instanceof Ping))
                    NetworkLogger.info("Broadcasting " + instruction.getClass().getSimpleName() + " to network");
                getTransmitter().broadcastMessage(message);
            } else {
                if (!(instruction instanceof Ping))
                    NetworkLogger.info("Sending " + instruction.getClass()
                                                               .getSimpleName() + " to " + Arrays.toString(receivers));
                getTransmitter().sendMessage(message, receivers);
            }
            if (instructionInfo.awaitsResponse())
                pendingInstructions.put(message.getInstructionUUID(), instruction);
        } else {
            NetworkLogger.warning("[" + sessionIdentifier + "] Instruction was dumped before sending");
        }
        return instruction.getResponse();
    }

    @Subscribe
    private void onMessage(MessageEvent messageEvent) {
        try {
            var message = messageEvent.getMessage();
            if (!isValidMessage(message)) {
                NetworkLogger.warning("Message is invalid");
                return;
            }

            var cachedInstructionData = getMessageFactory().getInstructionType(message.getInstructionID());
            if (cachedInstructionData == null) {
                NetworkLogger.warning("Message type is unknown");
                return;
            }

            var instructionType = cachedInstructionData.type();

            if (message.isInstruction())
                handleInstruction(message, cachedInstructionData);
            else if (message.isResponse())
                handleResponse(message);
            else
                NetworkLogger.warning("Message with type " + instructionType.getSimpleName() + " is neither an instruction nor a response");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void handleInstruction(Message message, MessageFactory.CachedInstructionData<?> instructionData) {
        InstructionInfo instructionInfo = getMessageFactory().findInstructionInfo(instructionData.type());
        Instruction<?> response = instructionData.instanceSupplier().get();
        var instructionType = instructionData.type();
        response.onReceive(new TransmittedData(message.getSender(), message.getSenderIdentifier(), message.dataToSend()));
        if (!instructionInfo.awaitsResponse()) {
            NetworkLogger.debug(instructionType.getSimpleName() + " does not await a response");
            return;
        }

        NetworkLogger.info("Received instruction of type: " + instructionType.getSimpleName());

        if (!(response instanceof SimpleInstruction<?> instructionImpl) || !(response instanceof Responder responder) || (pendingInstructions.containsKey(message.getSender()) && !responder.respondToItself()))
            return;
        instructionImpl.setNetworkParticipant(networkParticipant);
        NetworkLogger.info("[" + getSessionIdentifier() + "] Answering " + instructionType.getSimpleName());
        List<Object> responseData = ((Responder) response).respondToData(new TransmittedData(message.getSender(), message.getSenderIdentifier(), message.dataToSend()));
        if (responseData == null || responseData.size() == 0) {
            NetworkLogger.info("[" + getSessionIdentifier() + "] " + instructionType.getSimpleName() + " response is empty so it wont be sent");
            return;
        }
        sendResponse(message, responseData);
    }

    private void sendResponse(Message message, List<Object> responseData) {
        Message response = getMessageFactory().constructResponse(message.getInstructionID(), message.getInstructionUUID(), message.dataToSend(), responseData);
        NetworkLogger.info("[" + getSessionIdentifier() + "] Sending response to " + message
                .getSenderIdentifier());
        getTransmitter().sendMessage(response, message.getSender());
    }

    private void handleResponse(Message message) {
        NetworkLogger.info("[" + getSessionIdentifier() + "] Received response from" + message
                .getSenderIdentifier());
        if (!pendingInstructions.containsKey(message.getInstructionUUID()))
            return;
        Instruction<?> instructionLeft = pendingInstructions.get(message.getInstructionUUID());
        if (!(instructionLeft instanceof SimpleInstruction<?> instructionImpl) || !(instructionLeft instanceof Responder responder))
            return;
        NetworkLogger.info("Handling response");
        instructionImpl.setNetworkParticipant(networkParticipant);
        responder.onResponseReceive(new TransmittedData(getSessionUUID(), getSessionIdentifier(), message.dataToSend()), new TransmittedData(message.getSender(), message.getSenderIdentifier(), message.response()));
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
    public void postMessageEvent(String channelName, Message message) {
        eventBus.post(new MessageEventImpl(channelName, message));
    }

    private boolean isValidMessage(Message message) {
        return message.parameterContains(INSTRUCTION_IDENTIFIER) || message.parameterContains(RESPONSE_IDENTIFIER);
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
