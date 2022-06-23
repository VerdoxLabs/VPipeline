package de.verdox.vpipeline.impl.messaging;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import de.verdox.vpipeline.api.messaging.IMessageFactory;
import de.verdox.vpipeline.api.messaging.IMessagingService;
import de.verdox.vpipeline.api.messaging.ITransmitter;
import de.verdox.vpipeline.api.messaging.annotations.InstructionInfo;
import de.verdox.vpipeline.api.messaging.event.IMessageEvent;
import de.verdox.vpipeline.api.messaging.instruction.IInstruction;
import de.verdox.vpipeline.api.messaging.instruction.IResponder;
import de.verdox.vpipeline.api.messaging.instruction.Instruction;
import de.verdox.vpipeline.api.messaging.message.IMessage;
import de.verdox.vpipeline.api.messaging.message.MessageWrapper;
import de.verdox.vpipeline.impl.messaging.event.MessageEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 23:21
 */
public class MessagingService implements IMessagingService {
    private final Map<UUID, IInstruction<?>> pendingInstructions = new ConcurrentHashMap<>();

    private final EventBus eventBus;
    private final IMessageFactory messageFactory;

    private final ITransmitter transmitter;
    private final UUID sessionUUID;
    private final String sessionIdentifier;


    public MessagingService(String sessionIdentifier, ITransmitter transmitter) {
        this.sessionIdentifier = sessionIdentifier;
        this.transmitter = transmitter;
        this.eventBus = new EventBus();
        this.messageFactory = new MessageFactory(this);
        sessionUUID = UUID.randomUUID();
    }

    @Override
    public void sendInstruction(@NotNull IInstruction<?> instruction) {
        sendInstruction(instruction, new UUID[0]);
    }

    @Override
    public void sendInstruction(@NotNull IInstruction<?> instruction, UUID... receivers) {
        InstructionInfo instructionInfo = getMessageFactory().findInstructionInfo((Class<? extends IInstruction<?>>) instruction.getClass());
        UUID uuid = instruction.getUUID();

        IMessage message = getMessageFactory().constructMessage(instruction);
        if (message == null)
            return;
        if (!instruction.onSend(instruction.getData()))
            return;
        if (receivers.length == 0 || (receivers.length == 1 && receivers[0].equals("")))
            getTransmitter().broadcastMessage(message);
        else
            getTransmitter().sendMessage(message, receivers);
        if (instructionInfo.awaitsResponse())
            pendingInstructions.put(uuid, instruction);
    }

    @Override
    public ITransmitter getTransmitter() {
        return transmitter;
    }

    @Override
    public IMessageFactory getMessageFactory() {
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
    public boolean isOwnMessage(IMessage message) {
        return new MessageWrapper(message).getSenderUUID().equals(sessionUUID);
    }

    @Override
    public void postMessageEvent(String channelName, IMessage message) {
        eventBus.post(new MessageEvent(channelName, message));
    }

    @Override
    public void shutdown() {
        transmitter.shutdown();
    }

    @Subscribe
    private void onMessage(IMessageEvent messageEvent) {
        MessageWrapper messageWrapper = new MessageWrapper(messageEvent.getMessage());

        if (!isValidMessage(messageWrapper))
            return;

        Class<? extends Instruction<?>> instructionType = (Class<? extends Instruction<?>>) getMessageFactory().getInstructionType(messageWrapper.getInstructionID());
        if (instructionType == null)
            return;

        if (messageWrapper.isInstruction())
            handleInstruction(messageWrapper, instructionType);
        else if (messageWrapper.isResponse())
            handleResponse(messageWrapper);
    }

    private void handleInstruction(MessageWrapper messageWrapper, Class<? extends Instruction<?>> instructionType) {
        InstructionInfo instructionInfo = getMessageFactory().findInstructionInfo(instructionType);
        if (!instructionInfo.awaitsResponse())
            return;
        IInstruction<?> response = getMessageFactory().createInstruction(instructionType, messageWrapper.getInstructionUUID());
        if (!(response instanceof IResponder responder))
            return;
        if (pendingInstructions.containsKey(messageWrapper.getSenderUUID()) && !responder.respondToItself())
            return;
        Object[] responseData = ((IResponder) response).prepareResponse(messageWrapper.getData());
        if (responseData == null || responseData.length == 0)
            return;
        sendResponse(messageWrapper, responseData);
    }

    private void handleResponse(MessageWrapper messageWrapper) {
        if (!pendingInstructions.containsKey(messageWrapper.getInstructionUUID()))
            return;
        IInstruction<?> instructionLeft = pendingInstructions.get(messageWrapper.getInstructionUUID());
        if (!(instructionLeft instanceof IResponder responder))
            return;
        responder.onResponseReceive(messageWrapper.getData(), messageWrapper.getResponseData());
    }

    private void sendResponse(MessageWrapper messageWrapper, Object[] responseData) {
        IMessage response = getMessageFactory().constructResponse(messageWrapper.getInstructionID(), messageWrapper.getInstructionUUID(), messageWrapper.getParameters(), messageWrapper.getData(), responseData);
        getTransmitter().sendMessage(response, messageWrapper.getSenderUUID());
    }

    private boolean isValidMessage(MessageWrapper wrapper) {
        return wrapper.parameterContains(INSTRUCTION_IDENTIFIER) || wrapper.parameterContains(RESPONSE_IDENTIFIER);
    }
}
