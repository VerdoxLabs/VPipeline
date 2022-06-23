package de.verdox.vpipeline.api.messaging.instruction;

import java.util.List;
import java.util.UUID;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 23:12
 */
public interface IInstruction<T> extends ISender<T> {
    IInstruction<T> withData(Object... data);

    UUID getUUID();

    /**
     * Getter for API Users to fetch the runtime parameters of this specific instruction.
     * @return Parameters as String Array. Normally this output should equal the parameters() output.
     */
    String[] getParameters();

    /**
     * @return The data that will be sent with this instruction
     */
    Object[] getData();

    /**
     * @return The timestamp when the instruction was created.
     */
    long getCreationTimeStamp();

    /**
     * @return The list of data types that are accepted by this instruction.
     */
    List<Class<?>> dataTypes();

    /**
     * This method defines the instructions parameters. Normally implemented by a class with constant parameters.
     * @return The list of parameters to identify this instruction
     */
    List<String> parameters();
}
