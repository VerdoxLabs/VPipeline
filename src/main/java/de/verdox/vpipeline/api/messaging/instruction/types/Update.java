package de.verdox.vpipeline.api.messaging.instruction.types;

import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.messaging.annotations.InstructionInfo;
import de.verdox.vpipeline.api.messaging.instruction.TransmittedData;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Instruction to change data remotely. The sender will wait for confirmation.
 */
@InstructionInfo(awaitsResponse = true)
public abstract class Update extends Query<Boolean> {
    public Update(@NotNull UUID uuid) {
        super(uuid);
    }

    /**
     * Whichever client receives this update will call this method.
     * Will also be called on the sender. If it is done on the sender or cancelled
     * it won't be sent to the network.
     *
     * @param instructionData Instruction data
     * @return Returns whether the Update was completed
     */
    @NotNull
    protected abstract UpdateCompletion executeUpdate(TransmittedData instructionData);

    @Override
    public List<Object> respondToData(TransmittedData instructionData) {
        var result = executeUpdate(instructionData);
        Objects.requireNonNull(result);
        if (result.equals(UpdateCompletion.DONE))
            return List.of(true);
        else if (result.equals(UpdateCompletion.CANCELLED))
            return List.of(false);
        else
            return new LinkedList<>();
    }

    @Override
    protected Boolean interpretResponse(TransmittedData responseData) {
        return (Boolean) responseData.data().get(0);
    }


    @Override
    protected boolean shouldSend(TransmittedData instructionData) {
        var localResult = executeUpdate(instructionData);
        NetworkLogger.info("Local Result of " + getClass().getSimpleName() + ": " + localResult);
        Objects.requireNonNull(localResult);
        if (localResult.equals(UpdateCompletion.DONE) || localResult.equals(UpdateCompletion.CANCELLED)) {
            if (localResult.equals(UpdateCompletion.DONE)) {
                NetworkLogger.info("Update was executed locally.");
                getResponse().complete(instructionData.transmitter(), true);
            } else {
                NetworkLogger.info("Update was cancelled locally.");
                getResponse().complete(instructionData.transmitter(), false);
            }
            return false;
        }
        NetworkLogger.info("Sending " + getClass().getSimpleName() + " to other servers.");
        return true;
    }

    public enum UpdateCompletion {
        DONE,
        NOT_DONE,
        CANCELLED;

        public static UpdateCompletion fromBoolean(boolean value) {
            if (value)
                return DONE;
            else
                return NOT_DONE;
        }
    }
}
