package de.verdox.vpipeline.api.messaging.instruction.types;

import de.verdox.vpipeline.api.messaging.MessagingService;
import de.verdox.vpipeline.api.messaging.annotations.InstructionInfo;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Instruction to change data remotely. The sender will wait for confirmation.
 */
@InstructionInfo(awaitsResponse = true)
public abstract class Update extends Query<Update.UpdateCompletion> {
    public Update(@NotNull UUID uuid) {
        super(uuid);
    }

    @Override
    public final boolean isSufficientLocalResult(UpdateCompletion localResult) {
        return UpdateCompletion.DONE.equals(localResult);
    }

    public enum UpdateCompletion {
        DONE,
        NOT_DONE,
        CANCELLED;

        public static UpdateCompletion fromBoolean(boolean value) {
            return value ? DONE : NOT_DONE;
        }
    }

    @Override
    public void onResponseReceive(MessagingService messagingService, UpdateCompletion response) {

    }
}
