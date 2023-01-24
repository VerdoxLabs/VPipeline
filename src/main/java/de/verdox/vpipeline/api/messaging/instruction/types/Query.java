package de.verdox.vpipeline.api.messaging.instruction.types;

import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.messaging.annotations.InstructionInfo;
import de.verdox.vpipeline.api.messaging.instruction.Responder;
import de.verdox.vpipeline.api.messaging.instruction.SimpleInstruction;
import de.verdox.vpipeline.api.messaging.instruction.TransmittedData;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Instruction to query data remotely.
 *
 * @param <T> The data that is queried.
 */
@InstructionInfo(awaitsResponse = true)
public abstract class Query<T> extends SimpleInstruction<T> implements Responder {


    public Query(@NotNull UUID uuid) {
        super(uuid);
    }

    /**
     * Used to interpret the data received from a response to complete the CompletableFuture object
     *
     * @param responseData Data received as response
     * @return The interpreted response value
     */
    protected abstract T interpretResponse(TransmittedData responseData);

    @Override
    public void onReceive(TransmittedData transmittedData) {

    }

    @Override
    protected boolean shouldSend(TransmittedData instructionData) {
        var answer = respondToData(instructionData);
        if (answer != null && answer.size() >= 1) {
            NetworkLogger.debug("[" + getCurrentClient()
                    .messagingService()
                    .getSessionIdentifier() + "] Query was answered locally");
            var responseData = new TransmittedData(instructionData.transmitter(), instructionData.transmitterIdentifier(), answer);
            onResponseReceive(instructionData, responseData);
            return false;
        }
        return true;
    }

    /**
     * Executed whenever an answer to this query is received
     *
     * @param instructionData The original instruction data.
     * @param responseData    The response to the original instruction data
     */
    @Override
    public final void onResponseReceive(TransmittedData instructionData, TransmittedData responseData) {
        if (responseData.data() == null || responseData.data().size() == 0)
            return;
        response.complete(responseData.transmitter(), interpretResponse(responseData));
    }

    @Override
    public boolean respondToItself() {
        return true;
    }
}
