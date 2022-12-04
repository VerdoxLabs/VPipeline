package de.verdox.vpipeline.impl.messaging;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.NetworkParticipant;
import de.verdox.vpipeline.api.messaging.MessageFactory;
import de.verdox.vpipeline.api.messaging.MessagingService;
import de.verdox.vpipeline.api.messaging.Transmitter;
import de.verdox.vpipeline.api.messaging.annotations.InstructionInfo;
import de.verdox.vpipeline.api.messaging.event.MessageEvent;
import de.verdox.vpipeline.api.messaging.instruction.Instruction;
import de.verdox.vpipeline.api.messaging.instruction.Responder;
import de.verdox.vpipeline.api.messaging.instruction.SimpleInstruction;
import de.verdox.vpipeline.api.messaging.message.Message;
import de.verdox.vpipeline.api.messaging.message.MessageWrapper;
import de.verdox.vpipeline.impl.messaging.event.MessageEventImpl;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 23:21
 */
public class MessagingServiceImpl implements MessagingService {
    private final Map<UUID, Instruction<?>> pendingInstructions = new ConcurrentHashMap<>();

    private final EventBus eventBus;
    private final MessageFactory messageFactory;

    private final Transmitter transmitter;
    private final UUID sessionUUID;
    private final String sessionIdentifier;
    private NetworkParticipant networkParticipant;

    public MessagingServiceImpl(String sessionIdentifier, Transmitter transmitter) {
        this.sessionIdentifier = sessionIdentifier;
        this.transmitter = transmitter;
        this.eventBus = new EventBus();
        eventBus.register(this);
        this.messageFactory = new MessageFactoryImpl(this);
        sessionUUID = UUID.nameUUIDFromBytes(sessionIdentifier.getBytes());
    }

    public void setNetworkParticipant(NetworkParticipant networkParticipant) {
        this.networkParticipant = networkParticipant;
    }

    @Override
    public NetworkParticipant getNetworkParticipant() {
        return networkParticipant;
    }

    @Override
    public void sendInstruction(@NotNull Instruction<?> instruction) {
        sendInstruction(instruction, new UUID[0]);
    }

    @Override
    public void sendInstruction(@NotNull Instruction<?> instruction, UUID... receivers) {
        if (!(instruction instanceof SimpleInstruction<?> instructionImpl)) {
            NetworkLogger
                    .getLogger()
                    .info("[" + sessionIdentifier + "] Dumping unknown instruction");
            return;
        }
        NetworkLogger
                .getLogger()
                .severe("[" + sessionIdentifier + "] Sending instruction");
        instructionImpl.setNetworkParticipant(networkParticipant);

        InstructionInfo instructionInfo = getMessageFactory().findInstructionInfo((Class<? extends Instruction<?>>) instruction.getClass());
        UUID uuid = instruction.getUUID();

        Message message = getMessageFactory().constructMessage(instruction);
        Objects.requireNonNull(message);
        if (!instruction.onSend(instruction.getData())) {
            NetworkLogger
                    .getLogger()
                    .info("[" + sessionIdentifier + "] Dumping instruction");
            return;
        }
        if (receivers.length == 0)
            getTransmitter().broadcastMessage(message);
        else
            getTransmitter().sendMessage(message, receivers);
        if (instructionInfo.awaitsResponse()) {
            pendingInstructions.put(uuid, instruction);
            NetworkLogger
                    .getLogger()
                    .info("[" + sessionIdentifier + "] Awaiting response");
        }
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
    public void shutdown() {
        NetworkLogger.info("Shutting down message transmitter");
        transmitter.shutdown();
        NetworkLogger.info("MessagingService is offline");
    }

    @Subscribe
    private void onMessage(MessageEvent messageEvent) {
        MessageWrapper messageWrapper = new MessageWrapper(messageEvent.getMessage());
        if (!isValidMessage(messageWrapper))
            return;

        NetworkLogger
                .getLogger()
                .info("[" + sessionIdentifier + "] Message received on channel " + messageEvent.getChannelName() + " from server " + messageEvent
                        .getMessage()
                        .getSenderIdentifier());

        Class<? extends SimpleInstruction<?>> instructionType = (Class<? extends SimpleInstruction<?>>) getMessageFactory().getInstructionType(messageWrapper.getInstructionID());
        if (instructionType == null)
            return;

        if (messageWrapper.isInstruction()) {
            NetworkLogger
                    .getLogger()
                    .info("[" + sessionIdentifier + "]" + " Message is a " + instructionType.getSimpleName() + " instruction");
            handleInstruction(messageWrapper, instructionType);
        } else if (messageWrapper.isResponse()) {
            NetworkLogger
                    .getLogger()
                    .info("[" + sessionIdentifier + "]" + " Message is a " + instructionType.getSimpleName() + " response");
            handleResponse(messageWrapper);
        }
    }

    private void handleInstruction(MessageWrapper messageWrapper, Class<? extends SimpleInstruction<?>> instructionType) {
        InstructionInfo instructionInfo = getMessageFactory().findInstructionInfo(instructionType);
        if (!instructionInfo.awaitsResponse()) {
            NetworkLogger
                    .getLogger()
                    .info("[" + sessionIdentifier + "] " + instructionType.getSimpleName() + " does not await a response");
            return;
        }
        Instruction<?> response = getMessageFactory().createInstruction(instructionType, messageWrapper.getInstructionUUID());
        if (!(response instanceof SimpleInstruction<?> instructionImpl))
            return;
        instructionImpl.setNetworkParticipant(networkParticipant);
        if (!(response instanceof Responder responder))
            return;
        if (pendingInstructions.containsKey(messageWrapper.getSenderUUID()) && !responder.respondToItself())
            return;
        NetworkLogger
                .getLogger()
                .info("[" + sessionIdentifier + "] Answering " + instructionType.getSimpleName());
        Object[] responseData = ((Responder) response).answerQuery(messageWrapper.getData());
        if (responseData == null || responseData.length == 0) {
            NetworkLogger
                    .getLogger()
                    .info("[" + sessionIdentifier + "] " + instructionType.getSimpleName() + " response is empty so it wont be sent");
            return;
        }
        sendResponse(messageWrapper, responseData);
    }

    private void handleResponse(MessageWrapper messageWrapper) {
        if (!pendingInstructions.containsKey(messageWrapper.getInstructionUUID()))
            return;
        Instruction<?> instructionLeft = pendingInstructions.get(messageWrapper.getInstructionUUID());
        if (!(instructionLeft instanceof SimpleInstruction<?> instructionImpl))
            return;
        instructionImpl.setNetworkParticipant(networkParticipant);
        if (!(instructionLeft instanceof Responder responder))
            return;
        responder.onQueryAnswerReceive(messageWrapper.getData(), messageWrapper.getResponseData());
    }

    private void sendResponse(MessageWrapper messageWrapper, Object[] responseData) {
        Message response = getMessageFactory().constructResponse(messageWrapper.getInstructionID(), messageWrapper.getInstructionUUID(), messageWrapper.getParameters(), messageWrapper.getData(), responseData);
        NetworkLogger
                .getLogger()
                .info("[" + sessionIdentifier + "]" + " Sending response to " + messageWrapper
                        .message()
                        .getSenderIdentifier());
        getTransmitter().sendMessage(response, messageWrapper.getSenderUUID());
    }

    private boolean isValidMessage(MessageWrapper wrapper) {
        return wrapper.parameterContains(INSTRUCTION_IDENTIFIER) || wrapper.parameterContains(RESPONSE_IDENTIFIER);
    }
}
