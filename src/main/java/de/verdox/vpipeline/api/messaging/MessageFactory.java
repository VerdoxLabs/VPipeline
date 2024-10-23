package de.verdox.vpipeline.api.messaging;

import de.verdox.vpipeline.api.messaging.annotations.InstructionInfo;
import de.verdox.vpipeline.api.messaging.instruction.Instruction;

import java.util.function.Supplier;

/**
 * Used to construct messages for a {@link MessagingService}
 */
public interface MessageFactory {
    /**
     * Returns the linked {@link MessagingService}
     *
     * @return the messaging service
     */
    MessagingService getMessagingService();

    /**
     * Used to register an {@link Instruction} to the system.
     * Registration is needed to properly answer to messages that were received
     *
     * @param id               the unique id of the instruction
     * @param instructionType  the instruction class type
     * @param instanceSupplier a constructor
     * @param <T>              the instruction type
     */
    <T extends Instruction<?>> void registerInstructionType(int id, Class<? extends T> instructionType, Supplier<T> instanceSupplier);

    /**
     * Returns the instruction id by {@link Instruction} type
     *
     * @param type the type
     * @return the instruction id
     */
    int findInstructionID(Class<? extends Instruction<?>> type);

    /**
     * Returns the instruction id by providing an {@link Instruction}
     *
     * @param instruction the instruction
     * @return the instruction id
     */
    int findInstructionID(Instruction<?> instruction);

    /**
     * Returns the {@link CachedInstructionData} by its id
     *
     * @param id the id
     * @return the cached instruction data
     */
    CachedInstructionData<?> getCachedInstructionData(int id);

    /**
     * Checks if a particular {@link Instruction} type is registered
     * @param type the type
     * @return true if it is registered
     */
    boolean isTypeRegistered(Class<? extends Instruction<?>> type);

    /**
     * Returns the instruction info for a specific {@link Instruction} type
     * @param type the type
     * @return the instruction info
     */
    InstructionInfo findInstructionInfo(Class<? extends Instruction<?>> type);

    /**
     * Used to bundle all registration information of an instruction type
     *
     * @param type
     * @param instanceSupplier
     * @param <T>
     */
    record CachedInstructionData<T extends Instruction<?>>(Class<? extends T> type, Supplier<T> instanceSupplier) {
    }
}
