package de.verdox.vpipeline.api.messaging;

import de.verdox.vpipeline.api.messaging.annotations.InstructionInfo;
import de.verdox.vpipeline.api.messaging.instruction.Instruction;
import de.verdox.vpipeline.api.messaging.message.Message;

import java.util.UUID;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 19.06.2022 12:34
 */
public interface MessageFactory  {
    MessagingService getMessagingService();

    void registerInstructionType(int id, Class<? extends Instruction<?>> instructionType);

    Class<? extends Instruction<?>> getInstructionType(int id);

    Message constructMessage(Instruction<?> instruction);

    Message constructResponse(int instructionID, UUID instructionUUID, String[] arguments, Object[] instructionData, Object[] responseData);

    InstructionInfo findInstructionInfo(Class<? extends Instruction<?>> type);

    Instruction<?> createInstruction(Class<? extends Instruction<?>> type, UUID uuid);

    int findInstructionID(Class<? extends Instruction<?>> type);

    int findInstructionID(Instruction<?> instruction);
}
