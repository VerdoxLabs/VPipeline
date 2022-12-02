package model;

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
    protected @NotNull UpdateCompletion executeUpdateRemotely(Object[] instructionData) {
        var string = (String) instructionData[0];
        if (string.equals("example"))
            return UpdateCompletion.TRUE;
        return UpdateCompletion.FALSE;
    }

    @Override
    protected boolean executeUpdateLocally(Object[] instructionData) {
        return false;
    }
}
