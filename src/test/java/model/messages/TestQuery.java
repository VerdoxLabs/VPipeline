package model.messages;

import de.verdox.vpipeline.api.messaging.instruction.TransmittedData;
import de.verdox.vpipeline.api.messaging.instruction.types.Query;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class TestQuery extends Query<String> {
    public TestQuery(@NotNull UUID uuid) {
        super(uuid);
    }

    @Override
    public List<Object> respondToData(TransmittedData instructionData) {
        if (isOwnTransmittedData(instructionData))
            return null;
        else
            return List.of("test");
    }

    @Override
    protected String interpretResponse(TransmittedData responseData) {
        return (String) responseData.data().get(0);
    }
}
