package de.verdox.vpipeline.impl.messaging;

import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.messaging.MessageFactory;
import de.verdox.vpipeline.api.messaging.MessagingService;
import de.verdox.vpipeline.api.messaging.annotations.InstructionInfo;
import de.verdox.vpipeline.api.messaging.instruction.Instruction;
import de.verdox.vpipeline.api.messaging.message.Message;
import de.verdox.vpipeline.impl.messaging.message.MessageImpl;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class MessageFactoryImpl implements MessageFactory {

    private final MessagingService messagingService;
    private final Map<Integer, CachedInstructionData<?>> instructionTypes = new ConcurrentHashMap<>();

    public MessageFactoryImpl(MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @Override
    public MessagingService getMessagingService() {
        return messagingService;
    }

    @Override
    public <T extends Instruction<?>> void registerInstructionType(int id, Class<? extends T> instructionType, Supplier<T> instanceSupplier) {
        if (instructionTypes.containsKey(id))
            throw new IllegalStateException("Id already registered: " + id);
        instructionTypes.put(id, new CachedInstructionData<>(instructionType, instanceSupplier));
    }

    @Override
    public CachedInstructionData<?> getInstructionType(int id) {
        return instructionTypes.getOrDefault(id, null);
    }

    @Override
    public Message constructMessage(Instruction<?> instruction) {
        if (instruction.getData() == null || instruction.getData().length == 0)
            throw new IllegalStateException("You can't send empty instructions");

        int id = findInstructionID(instruction);

        if (id <= -1)
            throw new IllegalStateException("Sending an Instruction that has not been registered: " + instruction
                    .getClass()
                    .getSimpleName());

        NetworkLogger
                .debug("[" + messagingService.getSessionIdentifier() + "] Constructing Message with " + messagingService.getSessionUUID());

        return new MessageImpl(messagingService.getSessionUUID(), instruction.getUUID(), messagingService.getSessionIdentifier(), id)
                .addParameter(MessagingService.INSTRUCTION_IDENTIFIER)
                .addDataToSend(List.of(instruction.getData()));
    }

    @Override
    public Message constructResponse(int instructionID, UUID instructionUUID, List<Object> instructionData, List<Object> responseData) {
        Objects.requireNonNull(instructionUUID);
        Objects.requireNonNull(instructionData);
        Objects.requireNonNull(responseData);

        var cachedInstructionData = getInstructionType(instructionID);

        Class<? extends Instruction<?>> instructionType = cachedInstructionData.type();
        if (instructionType == null)
            return null;

        NetworkLogger
                .getLogger()
                .info("[" + messagingService.getSessionIdentifier() + "] Constructing Response with " + messagingService.getSessionUUID());

        return new MessageImpl(messagingService.getSessionUUID(), instructionUUID, messagingService.getSessionIdentifier(), instructionID)
                .addParameter(MessagingService.RESPONSE_IDENTIFIER)
                .addDataToSend(instructionData)
                .addResponses(responseData);
    }

    @Override
    public InstructionInfo findInstructionInfo(Class<? extends Instruction<?>> type) {
        InstructionInfo instructionInfo = type.getAnnotation(InstructionInfo.class);
        if (instructionInfo == null)
            throw new IllegalStateException("Class " + type.getName() + " is missing InstructionInfo Annotation");
        return instructionInfo;
    }

    @Override
    public Instruction<?> createInstruction(Class<? extends Instruction<?>> type, UUID uuid) {
        try {
            var constructor = type.getDeclaredConstructor(UUID.class);
            constructor.setAccessible(true);
            return constructor.newInstance(uuid);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            e.printStackTrace();
            throw new IllegalStateException(type.getSimpleName() + " needs a constructor (UUID)");
        }
    }

    @Override
    public int findInstructionID(Class<? extends Instruction<?>> type) {
        for (Integer integer : instructionTypes.keySet()) {
            Class<? extends Instruction<?>> foundType = instructionTypes.get(integer).type();
            if (type.equals(foundType))
                return integer;
        }
        return -1;
    }

    @Override
    public int findInstructionID(Instruction<?> instruction) {
        return findInstructionID((Class<? extends Instruction<?>>) instruction.getClass());
    }
}
