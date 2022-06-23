package de.verdox.vpipeline.api.messaging.message;

import java.util.UUID;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 23:00
 */
public interface IMessage {
    UUID getSender();

    String getSenderIdentifier();

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
            throw new ClassCastException("Cannot cast data in index[" + index + "] to " + type + "!");
        return type.cast(dataToSend()[index]);
    }
}
