package de.verdox.vpipeline.api.messaging.parts.transmitter;

import de.verdox.vpipeline.api.messaging.MessagingService;
import de.verdox.vpipeline.api.messaging.Transmitter;
import de.verdox.vpipeline.api.messaging.instruction.Instruction;

import java.util.UUID;

public class DummyTransmitter implements Transmitter {
    private MessagingService messagingService;
    @Override
    public long sendMessage(Instruction<?> message, UUID... receivers) {
        return broadcastMessage(message);
    }

    @Override
    public long broadcastMessage(Instruction<?> message) {
        messagingService.postMessageEvent("global", message);
        return 1;
    }

    @Override
    public void setMessagingService(MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @Override
    public long getNetworkTransmitterAmount() {
        return 1;
    }

    @Override
    public void shutdown() {

    }
}
