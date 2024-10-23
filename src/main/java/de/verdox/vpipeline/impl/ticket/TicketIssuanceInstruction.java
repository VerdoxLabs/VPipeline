package de.verdox.vpipeline.impl.ticket;

import de.verdox.vpipeline.api.messaging.MessagingService;
import de.verdox.vpipeline.api.messaging.instruction.types.Ping;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class TicketIssuanceInstruction extends Ping {
    private final byte[] data;
    public TicketIssuanceInstruction(@NotNull UUID uuid, byte[] data) {
        super(uuid);
        this.data = data;
    }

    @Override
    public void onPingReceive(MessagingService messagingService) {
        TicketPropagatorImpl ticketPropagator = (TicketPropagatorImpl) messagingService.getTicketPropagator();
        ticketPropagator.reconstructTicket(getUuid(), data);
    }
}
