package de.verdox.vpipeline.api.messaging.instruction.types;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 22.06.2022 21:15
 */
public abstract class Update extends Query<Boolean> {
    public Update(@NotNull UUID uuid) {
        super(uuid);
    }

    @NotNull
    protected abstract UpdateCompletion executeUpdate(Object[] instructionData);

    @Override
    public void onResponseReceive(Object[] instructionData, Object[] responseData) {
        getFuture().complete((Boolean) responseData[0]);
    }

    @Override
    public Object[] prepareResponse(Object[] instructionData) {
        return executeUpdate(instructionData) == UpdateCompletion.FALSE ? new Object[]{false} : new Object[]{true};
    }

    @Override
    public boolean onSend(Object[] instructionData) {
        return !executeUpdate(instructionData).toValue();
    }

    public enum UpdateCompletion {
        TRUE(true),
        FALSE(false),
        NOTHING(false),
        ;
        private final boolean value;

        UpdateCompletion(boolean value) {
            this.value = value;
        }

        public boolean toValue() {
            return value;
        }
    }
}
