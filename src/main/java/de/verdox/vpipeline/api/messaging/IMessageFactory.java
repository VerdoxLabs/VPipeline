package de.verdox.vpipeline.api.messaging;

import de.verdox.vpipeline.api.messaging.annotations.InstructionInfo;
import de.verdox.vpipeline.api.messaging.instruction.IInstruction;
import de.verdox.vpipeline.api.messaging.message.IMessage;

import java.util.UUID;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 19.06.2022 12:34
 */
public interface IMessageFactory {
    IMessagingService getMessagingService();

    void registerInstructionType(int id, Class<? extends IInstruction<?>> instructionType);

    Class<? extends IInstruction<?>> getInstructionType(int id);


    IMessage constructMessage(IInstruction<?> instruction);

    IMessage constructResponse(int instructionID, UUID instructionUUID, String[] arguments, Object[] instructionData, Object[] responseData);

    InstructionInfo findInstructionInfo(Class<? extends IInstruction<?>> type);

    IInstruction<?> createInstruction(Class<? extends IInstruction<?>> type, UUID uuid);

    int findInstructionID(Class<? extends IInstruction<?>> type);

    int findInstructionID(IInstruction<?> instruction);
}
