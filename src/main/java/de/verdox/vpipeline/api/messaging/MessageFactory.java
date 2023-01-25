package de.verdox.vpipeline.api.messaging;

import de.verdox.vpipeline.api.messaging.annotations.InstructionInfo;
import de.verdox.vpipeline.api.messaging.instruction.Instruction;

import java.util.function.Supplier;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 19.06.2022 12:34
 */
public interface MessageFactory {
    MessagingService getMessagingService();

    <T extends Instruction<?>> void registerInstructionType(int id, Class<? extends T> instructionType, Supplier<T> instanceSupplier);
    int findInstructionID(Class<? extends Instruction<?>> type);
    int findInstructionID(Instruction<?> instruction);
    CachedInstructionData<?> getInstructionType(int id);
    boolean isTypeRegistered(Class <? extends Instruction<?>> type);
    InstructionInfo findInstructionInfo(Class <? extends Instruction<?>> type);

    record CachedInstructionData<T extends Instruction<?>>(Class<? extends T> type, Supplier<T> instanceSupplier) {
    }
}
