package de.verdox.vpipeline.api.messaging.instruction.types;

import de.verdox.vpipeline.api.messaging.MessagingService;
import de.verdox.vpipeline.api.messaging.annotations.InstructionInfo;
import de.verdox.vpipeline.api.messaging.instruction.AbstractInstruction;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Instruction that works only one way. The sender won't wait for any confirmation.
 */

@InstructionInfo(awaitsResponse = false)
public abstract class Ping extends AbstractInstruction<Boolean> {

    public Ping(@NotNull UUID uuid) {
        super(uuid);
    }

    public abstract void onPingReceive(MessagingService messagingService);

    @Override
    public boolean onSend(MessagingService messagingService, long receiversAmount) {
        return true;
    }

    @Override
    public Boolean onInstructionReceive(MessagingService messagingService) {
        onPingReceive(messagingService);
        return false;
    }

    @Override
    public void onResponseReceive(MessagingService messagingService, Boolean response) {}
}
