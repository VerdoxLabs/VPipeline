package de.verdox.vpipeline.api.messaging.instruction;

import de.verdox.vpipeline.api.NetworkParticipant;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 23:16
 */
public abstract class SimpleInstruction<T> implements Instruction<T> {

    private final UUID uuid;
    private final String[] parameters;
    private final List<Class<?>> types;
    private final long creationTimeStamp;
    private Object[] data;
    private NetworkParticipant networkParticipant;

    private UUID sessionUUID;

    public SimpleInstruction(@NotNull UUID uuid) {
        Objects.requireNonNull(uuid, "uuid can't be null!");
        this.uuid = uuid;
        this.parameters = instructionPath().toArray(String[]::new);
        this.types = instructionDataTypes();
        this.creationTimeStamp = System.currentTimeMillis();
    }

    @Override
    public Instruction<T> withData(Object... data) {
        if (types != null) {
            if (data.length != types.size())
                throw new IllegalStateException("Wrong Input Parameter Length for " + getClass().getSimpleName() + " [" + instructionDataTypes().size() + "]");
            for (int i = 0; i < types.size(); i++) {
                Class<?> type = types.get(i);
                Object datum = data[i];
                if (!type.isAssignableFrom(datum.getClass()))
                    throw new IllegalStateException(datum + " is not type or subtype of " + type.getName());

            }
        }
        this.data = data;
        return this;
    }

    @Override
    public boolean onSend(Object[] instructionData) {
        return true;
    }

    public void registerSessionUUID(UUID sessionUUID) {
        this.sessionUUID = sessionUUID;
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

    public void setNetworkParticipant(NetworkParticipant networkParticipant) {
        this.networkParticipant = networkParticipant;
    }

    @Override
    public List<Class<?>> instructionDataTypes() {
        return null;
    }

    @Override
    public List<String> instructionPath() {
        return List.of(getClass().getSimpleName());
    }

    @Override
    public NetworkParticipant getCurrentClient() {
        return networkParticipant;
    }

    public static <T extends SimpleInstruction<?>> T createInstruction(Class<? extends T> type) {
        try {
            return type.getDeclaredConstructor(UUID.class).newInstance(UUID.randomUUID());
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
