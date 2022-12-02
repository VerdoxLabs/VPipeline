package de.verdox.vpipeline.api.messaging.instruction.types;

import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.messaging.annotations.InstructionInfo;
import de.verdox.vpipeline.api.messaging.instruction.Responder;
import de.verdox.vpipeline.api.messaging.instruction.SimpleInstruction;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 23.06.2022 00:49
 */

/**
 * Instruction that works only one way. The sender won't wait for any confirmation.
 */

@InstructionInfo(awaitsResponse = false)
public abstract class Ping extends SimpleInstruction<Boolean> implements Responder {
    private final FutureResponse<Boolean> future = new FutureResponse<>();

    public Ping(@NotNull UUID uuid) {
        super(uuid);
    }

    public abstract void onPingReceive(Object[] instructionData);

    @Override
    public final boolean onSend(Object[] instructionData) {
        future.complete(true);
        return true;
    }

    @Override
    public final Object[] answerQuery(Object[] instructionData) {
        onPingReceive(instructionData);
        return new Object[]{true};
    }

    @Override
    public final void onQueryAnswerReceive(Object[] instructionData, Object[] responseData) {
        future.complete(true);
    }

    @Override
    public final FutureResponse<Boolean> getFuture() {
        return future;
    }

    @Override
    public final boolean respondToItself() {
        return false;
    }
}
