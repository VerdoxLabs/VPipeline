package de.verdox.vpipeline.api.messaging.instruction;

import java.util.UUID;

public record TransmittedData(UUID transmitter, String transmitterIdentifier, Object[] data) {

    public <R> R getObject(int index, Class<? extends R> type) {
        if (index >= data.length)
            throw new IllegalArgumentException("Index " + index + " out of bounds for transmitted data with length " + data.length);
        var foundObject = data[index];
        try {
            return type.cast(foundObject);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Object at index " + index + " is not of type " + type.getSimpleName() + " but is " + foundObject);
        }
    }

}
