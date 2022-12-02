package de.verdox.vpipeline.api.messaging.instruction.types;

import de.verdox.vpipeline.api.messaging.annotations.InstructionInfo;
import org.jetbrains.annotations.NotNull;

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
     * Will be executed on a remote machine.
     *
     * @param instructionData Instruction data
     * @return Returns whether the Update was completed
     */
    @NotNull
    protected abstract UpdateCompletion executeUpdateRemotely(Object[] instructionData);

    /**
     * If the update was completed locally it won't be executed remotely.
     *
     * @param instructionData Instruction data
     * @return Returns whether the Update was completed locally
     */
    protected abstract boolean executeUpdateLocally(Object[] instructionData);

    @Override
    public Object[] answerQuery(Object[] instructionData) {
        return executeUpdateRemotely(instructionData) == UpdateCompletion.FALSE ? new Object[]{false} : new Object[]{true};
    }

    @Override
    protected Boolean interpretResponse(Object[] responseData) {
        return (Boolean) responseData[0];
    }

    @Override
    public boolean onSend(Object[] instructionData) {
        var localResult = executeUpdateLocally(instructionData);
        if (localResult)
            getFuture().complete(true);
        return !localResult;
    }

    public enum UpdateCompletion {
        TRUE(true),
        FALSE(false),
        NOTHING(false),
        ;
        private final boolean value;

        UpdateCompletion(boolean value) {
            this.value = value;
        }

        public boolean toValue() {
            return value;
        }
    }
}
