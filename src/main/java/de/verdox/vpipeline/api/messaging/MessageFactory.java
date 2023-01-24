package de.verdox.vpipeline.api.messaging;

import de.verdox.vpipeline.api.messaging.annotations.InstructionInfo;
import de.verdox.vpipeline.api.messaging.instruction.Instruction;
import de.verdox.vpipeline.api.messaging.message.Message;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 19.06.2022 12:34
 */
public interface MessageFactory {
    MessagingService getMessagingService();

    <T extends Instruction<?>> void registerInstructionType(int id, Class<? extends T> instructionType, Supplier<T> instanceSupplier);

    CachedInstructionData<?> getInstructionType(int id);

    Message constructMessage(Instruction<?> instruction);

    Message constructResponse(int instructionID, UUID instructionUUID, List<Object> instructionData, List<Object> responseData);

    InstructionInfo findInstructionInfo(Class<? extends Instruction<?>> type);

    Instruction<?> createInstruction(Class<? extends Instruction<?>> type, UUID uuid);

    int findInstructionID(Class<? extends Instruction<?>> type);

    int findInstructionID(Instruction<?> instruction);

    record CachedInstructionData<T extends Instruction<?>>(Class<? extends T> type, Supplier<T> instanceSupplier) {
    }
}
