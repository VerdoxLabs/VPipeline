package de.verdox.vpipeline.api.messaging.instruction;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 23:16
 */
public abstract class Instruction<T> implements IInstruction<T>{

    private final UUID uuid;
    private final String[] parameters;
    private final List<Class<?>> types;
    private final long creationTimeStamp;
    private Object[] data;

    public Instruction(@NotNull UUID uuid){
        Objects.requireNonNull(uuid, "uuid can't be null!");
        this.uuid = uuid;
        this.parameters = parameters().toArray(String[]::new);
        this.types = dataTypes();
        this.creationTimeStamp = System.currentTimeMillis();
    }

    @Override
    public IInstruction<T> withData(Object... data) {
        if (data.length != types.size())
            throw new IllegalStateException("Wrong Input Parameter Length for " + getClass().getSimpleName() + " [" + dataTypes().size() + "]");
        for (int i = 0; i < types.size(); i++) {
            Class<?> type = types.get(i);
            Object datum = data[i];
            if (!type.isAssignableFrom(datum.getClass()))
                throw new IllegalStateException(datum + " is not type or subtype of " + type.getName());

        }
        this.data = data;
        return this;
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public String[] getParameters() {
        return parameters;
    }

    @Override
    public Object[] getData() {
        return data;
    }

    @Override
    public long getCreationTimeStamp() {
        return creationTimeStamp;
    }
}
