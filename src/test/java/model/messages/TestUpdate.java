package model.messages;

import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.messaging.MessagingService;
import de.verdox.vpipeline.api.messaging.instruction.types.Update;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class TestUpdate extends Update {

    public String name;

    public TestUpdate(@NotNull UUID uuid) {
        super(uuid);
    }

    @Override
    public Update.UpdateCompletion onInstructionReceive(MessagingService messagingService) {
        if (name.equals("Hans"))
            return UpdateCompletion.DONE;
        else
            return null;
    }

    @Override
    public void onResponseReceive(MessagingService messagingService, Update.UpdateCompletion response) {
        NetworkLogger.info("Received response: " + response);
    }
}
