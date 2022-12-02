package model;

import de.verdox.vpipeline.api.messaging.instruction.types.Query;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class TestQuery extends Query<String> {
    public TestQuery(@NotNull UUID uuid) {
        super(uuid);
    }

    @Override
    protected String interpretResponse(Object[] responseData) {
        return (String) responseData[0];
    }

    @Override
    public Object[] answerQuery(Object[] instructionData) {
        return new Object[]{"TestQuery Result"};
    }
}
