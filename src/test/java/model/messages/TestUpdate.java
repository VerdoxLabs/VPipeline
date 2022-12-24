package model.messages;

import de.verdox.vpipeline.api.messaging.instruction.TransmittedData;
import de.verdox.vpipeline.api.messaging.instruction.types.Update;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class TestUpdate extends Update {
    public TestUpdate(@NotNull UUID uuid) {
        super(uuid);
    }

    @Override
    public List<Class<?>> instructionDataTypes() {
        return List.of(String.class);
    }

    @Override
    protected @NotNull UpdateCompletion executeUpdate(TransmittedData instructionData) {
        if (isOwnTransmittedData(instructionData))
            return UpdateCompletion.NOT_DONE;
        return UpdateCompletion.DONE;
    }
}
