package de.verdox.vpipeline.impl.messaging;

import de.verdox.vpipeline.api.messaging.annotations.InstructionInfo;
import de.verdox.vpipeline.api.messaging.instruction.AbstractInstruction;
import de.verdox.vpipeline.api.messaging.instruction.Instruction;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class MessageFactoryImpl implements de.verdox.vpipeline.api.messaging.MessageFactory {

    private final MessagingServiceImpl messagingServiceImpl;
    private final Map<Integer, CachedInstructionData<?>> instructionTypes = new ConcurrentHashMap<>();
    private final Set<Class<? extends Instruction<?>>> registeredTypes = new HashSet<>();

    public MessageFactoryImpl(MessagingServiceImpl messagingServiceImpl) {
        this.messagingServiceImpl = messagingServiceImpl;
    }


    @Override
    public de.verdox.vpipeline.api.messaging.MessagingService getMessagingService() {
        return this.messagingServiceImpl;
    }

    @Override
    public <T extends Instruction<?>> void registerInstructionType(int id, Class<? extends T> instructionType, Supplier<T> instanceSupplier) {
        if (instructionTypes.containsKey(id))
            throw new IllegalStateException("Id already registered: " + id);
        instructionTypes.put(id, new CachedInstructionData<>(instructionType, instanceSupplier));
        registeredTypes.add(instructionType);
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
        return findInstructionID((Class<? extends AbstractInstruction<?>>) instruction.getClass());
    }

    @Override
    public CachedInstructionData<?> getCachedInstructionData(int id) {
        return instructionTypes.getOrDefault(id, null);
    }

    @Override
    public boolean isTypeRegistered(Class<? extends Instruction<?>> type) {
        return registeredTypes.contains(type);
    }

    @Override
    public InstructionInfo findInstructionInfo(Class<? extends Instruction<?>> type) {
        InstructionInfo instructionInfo = type.getAnnotation(InstructionInfo.class);
        if (instructionInfo == null)
            throw new IllegalStateException("Class " + type.getName() + " is missing InstructionInfo Annotation");
        return instructionInfo;
    }
}
