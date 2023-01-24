package de.verdox.vpipeline.api.messaging.instruction;

import de.verdox.vpipeline.api.NetworkParticipant;

import java.util.List;
import java.util.UUID;


public interface Instruction<T> extends Sender<T> {
    Instruction<T> withData(Object... data);

    UUID getUUID();

    /**
     * @return The data that will be sent with this instruction
     */
    Object[] getData();

    boolean isOwnTransmittedData(TransmittedData transmittedData);

    /**
     * @return The timestamp when the instruction was created.
     */
    long getCreationTimeStamp();

    /**
     * @return The list of data types that are accepted by this instruction.
     */
    List<Class<?>> instructionDataTypes();

    /**
     * This method defines the instructions parameters. Normally implemented by a class with constant parameters.
     *
     * @return The list of parameters to identify this instruction
     */
    List<String> instructionPath();

    NetworkParticipant getCurrentClient();
}
