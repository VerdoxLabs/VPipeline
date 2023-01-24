package de.verdox.vpipeline.api.messaging.instruction.types;

import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.messaging.annotations.InstructionInfo;
import de.verdox.vpipeline.api.messaging.instruction.Responder;
import de.verdox.vpipeline.api.messaging.instruction.SimpleInstruction;
import de.verdox.vpipeline.api.messaging.instruction.TransmittedData;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Instruction that works only one way. The sender won't wait for any confirmation.
 */

@InstructionInfo(awaitsResponse = false)
public abstract class Ping extends SimpleInstruction<Boolean> implements Responder {

    public Ping(@NotNull UUID uuid) {
        super(uuid);
    }

    public abstract void onPingReceive(TransmittedData instructionData);

    @Override
    protected boolean shouldSend(TransmittedData instructionData) {
        response.complete(instructionData.transmitter(), true);
        return true;
    }

    @Override
    public void onReceive(TransmittedData transmittedData) {
        onPingReceive(transmittedData);
    }

    @Override
    public List<Object> respondToData(TransmittedData instructionData) {
        return new LinkedList<>();
    }

    @Override
    public final void onResponseReceive(TransmittedData instructionData, TransmittedData responseData) {
        response.complete(responseData.transmitter(), true);
    }

    @Override
    public final boolean respondToItself() {
        return false;
    }
}
