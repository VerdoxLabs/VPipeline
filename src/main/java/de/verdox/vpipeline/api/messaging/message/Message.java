package de.verdox.vpipeline.api.messaging.message;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.UUID;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 23:00
 */
public interface Message extends Serializable {
    @NotNull
    UUID getSender();

    @NotNull
    String getSenderIdentifier();

    @NotNull
    String[] getParameters();

    Object[] dataToSend();

    default int size() {
        if (dataToSend() == null)
            return 0;
        return dataToSend().length;
    }

    default boolean isTypeOf(int index, Class<?> type) {
        if (index < 0 || index >= size())
            return false;
        return dataToSend()[index].getClass().equals(type);
    }

    default boolean isAssignableFrom(int index, Class<?> type) {
        if (index < 0 || index >= size())
            return false;
        return dataToSend()[index].getClass().isAssignableFrom(type);
    }

    default <T> T getData(int index, Class<? extends T> type) {
        if (!isTypeOf(index, type))
            throw new ClassCastException("Cannot cast data in index[" + index + "] to " + type + " because it is " + dataToSend()[index]
                    .getClass()
                    .getSimpleName());
        return type.cast(dataToSend()[index]);
    }
}
