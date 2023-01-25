package model.messages;

import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.messaging.MessagingService;
import de.verdox.vpipeline.api.messaging.instruction.types.Query;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class TestQuery extends Query<String> {
    public TestQuery(@NotNull UUID uuid) {
        super(uuid);
    }

    @Override
    public String onInstructionReceive(MessagingService messagingService) {
        return "test";
    }

    @Override
    public void onResponseReceive(MessagingService messagingService, String response) {
        NetworkLogger.info("Received query answer: " + response);
    }
}
