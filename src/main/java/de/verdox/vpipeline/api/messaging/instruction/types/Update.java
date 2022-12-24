package de.verdox.vpipeline.api.messaging.instruction.types;

import de.verdox.vpipeline.api.messaging.annotations.InstructionInfo;
import de.verdox.vpipeline.api.messaging.instruction.TransmittedData;
import org.jetbrains.annotations.NotNull;

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
    public Object[] respondToData(TransmittedData instructionData) {
        var result = executeUpdate(instructionData);
        Objects.requireNonNull(result);
        if (result.equals(UpdateCompletion.DONE))
            return new Object[]{true};
        else if (result.equals(UpdateCompletion.CANCELLED))
            return new Object[]{false};
        else
            return new Object[0];
    }

    @Override
    protected Boolean interpretResponse(TransmittedData responseData) {
        return (Boolean) responseData.data()[0];
    }

    @Override
    public boolean onSend(TransmittedData instructionData) {
        var localResult = executeUpdate(instructionData);
        Objects.requireNonNull(localResult);
        if (localResult.equals(UpdateCompletion.DONE) || localResult.equals(UpdateCompletion.CANCELLED)) {
            if (localResult.equals(UpdateCompletion.DONE))
                getResponse().complete(instructionData.transmitter(), true);
            else
                getResponse().complete(instructionData.transmitter(), false);
            // Don't send the update to the network since it is done.
            return false;
        }
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
                return CANCELLED;
        }
    }
}
