package de.verdox.vpipeline.api.messaging.message;

import de.verdox.vpipeline.api.messaging.MessagingService;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 23:00
 */
public interface Message extends Serializable {
    @NotNull
    UUID getSender();

    int getInstructionID();

    @NotNull
    UUID getInstructionUUID();

    @NotNull
    String getSenderIdentifier();

    @NotNull
    List<String> getParameters();

    @NotNull
    List<Object> dataToSend();

    @NotNull
    List<Object> response();

    default int size() {
        if (dataToSend() == null)
            return 0;
        return dataToSend().size();
    }

    default boolean isTypeOf(int index, Class<?> type) {
        if (index < 0 || index >= size())
            return false;
        return dataToSend().get(index).getClass().equals(type);
    }

    default boolean isAssignableFrom(int index, Class<?> type) {
        if (index < 0 || index >= size())
            return false;
        return dataToSend().get(index).getClass().isAssignableFrom(type);
    }

    default Number getNumber(int index) {
        return (Number) dataToSend().get(index);
    }

    default UUID getUUID(int index) {
        return UUID.fromString((String) dataToSend().get(index));
    }

    default <T> T getData(int index, Class<? extends T> type) {
        if (Number.class.isAssignableFrom(type))
            throw new IllegalStateException("To get a number type please use getNumber");
        if (UUID.class.isAssignableFrom(type))
            return (T) getUUID(index);

        if (!isTypeOf(index, type))
            throw new ClassCastException("Cannot cast data in index[" + index + "] to " + type + " because it is " + dataToSend()
                    .get(index)
                    .getClass()
                    .getSimpleName());
        return type.cast(dataToSend().get(index));
    }

    default boolean isInstruction(){
        return parameterContains(MessagingService.INSTRUCTION_IDENTIFIER);
    }

    default boolean isResponse() {
        return parameterContains(MessagingService.RESPONSE_IDENTIFIER);
    }

    default boolean isValidMessage() {
        return isInstruction() || isResponse();
    }

    default boolean parameterContains(String... parameters) {
        if (getParameters() == null)
            return false;
        for (int i = 0; i < getParameters().size(); i++) {
            String messageParameter = getParameters().get(i);
            if (i >= parameters.length)
                continue;
            String neededParameter = parameters[i];
            if (!messageParameter.equals(neededParameter))
                return false;
        }
        return true;
    }
}
