package de.verdox.vpipeline.api.messaging.instruction.types;

import de.verdox.vpipeline.api.messaging.annotations.InstructionInfo;
import de.verdox.vpipeline.api.messaging.instruction.IResponder;
import de.verdox.vpipeline.api.messaging.instruction.Instruction;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 22.06.2022 21:15
 */

@InstructionInfo(awaitsResponse = true)
public abstract class Query<T> extends Instruction<T> implements IResponder {
    private final CompletableFuture<T> future = new CompletableFuture<>();

    public Query(@NotNull UUID uuid) {
        super(uuid);
    }

    @Override
    public boolean onSend(Object[] instructionData) {
        return true;
    }

    @Override
    public CompletableFuture<T> getFuture() {
        return future;
    }
}
