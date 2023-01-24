package de.verdox.vpipeline.api.messaging.instruction;

import de.verdox.vpipeline.api.NetworkLogger;

import java.util.List;
import java.util.UUID;

public record TransmittedData(UUID transmitter, String transmitterIdentifier, List<Object> data) {

    public <R> R getObject(int index, Class<? extends R> type) {
        if (index >= data.size())
            throw new IllegalArgumentException("Index " + index + " out of bounds for transmitted data with length " + data.size());
        var foundObject = data.get(index);

        if (UUID.class.isAssignableFrom(type)) {
            try {
                return type.cast(foundObject);
            } catch (ClassCastException e) {
                NetworkLogger.info(type.getSimpleName());
                return (R) UUID.fromString(getObject(index, String.class));
            }
        }

        if (Enum.class.isAssignableFrom(type))
            throw new IllegalStateException("Please use getEnum to get a number from transmitted data");

        try {
            if (Number.class.isAssignableFrom(type))
                throw new IllegalStateException("Please use getNumber to get a number from transmitted data");


            return type.cast(foundObject);
        } catch (ClassCastException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Object at index " + index + " is not of type " + type.getSimpleName() + " but is " + foundObject);
        }
    }

    public <R extends Enum<R>> R getEnum(int index, Class<R> enumType) {
        var foundObject = data.get(index);
        try {
            return enumType.cast(foundObject);
        } catch (ClassCastException e) {
            return Enum.valueOf(enumType, getObject(index, String.class));
        }
    }

    public Number getNumber(int index) {
        if (index >= data.size())
            throw new IllegalArgumentException("Index " + index + " out of bounds for transmitted data with length " + data.size());
        var foundObject = data.get(index);
        return Number.class.cast(foundObject);
    }

}
