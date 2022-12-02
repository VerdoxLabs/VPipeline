package de.verdox.vpipeline.api.messaging.instruction.types;

import de.verdox.vpipeline.api.messaging.annotations.InstructionInfo;
import de.verdox.vpipeline.api.messaging.instruction.Responder;
import de.verdox.vpipeline.api.messaging.instruction.SimpleInstruction;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 22.06.2022 21:15
 */

/**
 * Instruction to query data remotely.
 *
 * @param <T> The data that is queried.
 */
@InstructionInfo(awaitsResponse = true)
public abstract class Query<T> extends SimpleInstruction<T> implements Responder {
    private final FutureResponse<T> future = new FutureResponse<>();

    public Query(@NotNull UUID uuid) {
        super(uuid);
    }

    protected abstract T interpretResponse(Object[] responseData);

    @Override
    public final void onQueryAnswerReceive(Object[] instructionData, Object[] responseData) {
        future.complete(interpretResponse(responseData));
    }

    @Override
    public FutureResponse<T> getFuture() {
        return future;
    }
}
