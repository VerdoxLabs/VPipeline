package de.verdox.vpipeline.api.messaging.instruction.types;

import de.verdox.vpipeline.api.messaging.MessagingService;
import de.verdox.vpipeline.api.messaging.annotations.InstructionInfo;
import de.verdox.vpipeline.api.messaging.instruction.AbstractInstruction;
import de.verdox.vpipeline.impl.messaging.ResponseCollectorImpl;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Instruction to query data remotely.
 *
 * @param <T> The data that is queried.
 */
@InstructionInfo(awaitsResponse = true)
public abstract class Query<T> extends AbstractInstruction<T> {

    public Query(@NotNull UUID uuid) {
        super(uuid);
    }

    @Override
    public final boolean onSend(MessagingService messagingService, long receiversAmount) {
        var localResult = onInstructionReceive(messagingService);
        if (isSufficientLocalResult(localResult)) {
            this.setResponseToSend(localResult);
            onResponseReceive(messagingService, localResult);
            return false;
        }
        return true;
    }

    public boolean isSufficientLocalResult(T localResult) {
        return localResult != null;
    }
}
