package de.verdox.vpipeline.api.messaging.instruction.types;

import de.verdox.vpipeline.api.messaging.instruction.IResponder;
import de.verdox.vpipeline.api.messaging.instruction.Instruction;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 23.06.2022 00:49
 */
public abstract class Ping extends Instruction<Boolean> implements IResponder {
    private final CompletableFuture<Boolean> future = new CompletableFuture<>();

    public Ping(@NotNull UUID uuid) {
        super(uuid);
    }

    public abstract void onPingReceive(Object[] instructionData);

    @Override
    public final boolean onSend(Object[] instructionData) {
        return true;
    }

    @Override
    public final Object[] prepareResponse(Object[] instructionData) {
        onPingReceive(instructionData);
        return new Object[0];
    }

    @Override
    public final void onResponseReceive(Object[] instructionData, Object[] responseData) {}

    @Override
    public final CompletableFuture<Boolean> getFuture() {
        return future;
    }

    @Override
    public final boolean respondToItself() {
        return false;
    }
}
