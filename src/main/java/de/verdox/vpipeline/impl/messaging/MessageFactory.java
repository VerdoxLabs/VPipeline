package de.verdox.vpipeline.impl.messaging;

import de.verdox.vpipeline.api.messaging.IMessageFactory;
import de.verdox.vpipeline.api.messaging.IMessagingService;
import de.verdox.vpipeline.api.messaging.annotations.InstructionInfo;
import de.verdox.vpipeline.api.messaging.instruction.IInstruction;
import de.verdox.vpipeline.api.messaging.message.IMessage;
import de.verdox.vpipeline.impl.messaging.message.SimpleMessageBuilder;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 19.06.2022 12:51
 */
public class MessageFactory implements IMessageFactory {

    private final IMessagingService messagingService;
    private final Map<Integer, Class<? extends IInstruction<?>>> instructionTypes = new ConcurrentHashMap<>();

    public MessageFactory(IMessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @Override
    public IMessagingService getMessagingService() {
        return messagingService;
    }

    @Override
    public void registerInstructionType(int id, Class<? extends IInstruction<?>> instructionType) {
        if (instructionTypes.containsKey(id))
            throw new IllegalStateException("Id already registered: " + id);
        instructionTypes.put(id, instructionType);
    }

    @Override
    public Class<? extends IInstruction<?>> getInstructionType(int id) {
        return instructionTypes.getOrDefault(id, null);
    }

    @Override
    public IMessage constructMessage(IInstruction<?> instruction) {
        if (instruction.getData() == null || instruction.getData().length == 0)
            throw new IllegalStateException("You can't send empty instructions");

        int id = findInstructionID(instruction);

        if (id <= -1)
            throw new IllegalStateException("Sending an Instruction that has not been registered: " + instruction.getClass().getSimpleName());

        return new SimpleMessageBuilder(messagingService.getSessionUUID(), messagingService.getSessionIdentifier())
                .withParameters(IMessagingService.INSTRUCTION_IDENTIFIER)
                .withData(getMessagingService().getSessionUUID(), id, instruction.getUUID(), instruction.getParameters(), instruction.getData())
                .constructMessage();
    }

    @Override
    public IMessage constructResponse(int instructionID, UUID instructionUUID, String[] arguments, Object[] instructionData, Object[] responseData) {
        Objects.requireNonNull(instructionUUID);
        Objects.requireNonNull(arguments);
        Objects.requireNonNull(instructionData);
        Objects.requireNonNull(responseData);

        Class<? extends IInstruction<?>> instructionType = getInstructionType(instructionID);
        if (instructionType == null)
            return null;
        return new SimpleMessageBuilder(messagingService.getSessionUUID(), messagingService.getSessionIdentifier())
                .withParameters(IMessagingService.RESPONSE_IDENTIFIER)
                .withData(getMessagingService().getSessionUUID(), instructionID, instructionUUID, arguments, instructionData, responseData)
                .constructMessage();
    }

    @Override
    public InstructionInfo findInstructionInfo(Class<? extends IInstruction<?>> type) {
        InstructionInfo instructionInfo = type.getAnnotation(InstructionInfo.class);
        if (instructionInfo == null)
            throw new IllegalStateException("Class " + type.getName() + " is missing InstructionInfo Annotation");
        return instructionInfo;
    }

    @Override
    public IInstruction<?> createInstruction(Class<? extends IInstruction<?>> type, UUID uuid) {
        try {
            return type.getConstructor(UUID.class).newInstance(uuid);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalStateException(type.getSimpleName() + " needs a constructor (UUID)");
        }
    }

    @Override
    public int findInstructionID(Class<? extends IInstruction<?>> type) {
        for (Integer integer : instructionTypes.keySet()) {
            Class<? extends IInstruction<?>> foundType = instructionTypes.get(integer);
            if (type.equals(foundType))
                return integer;
        }
        return -1;
    }

    @Override
    public int findInstructionID(IInstruction<?> instruction) {
        return findInstructionID((Class<? extends IInstruction<?>>) instruction.getClass());
    }


}
