package de.verdox.vpipeline.impl.ticket;

import de.verdox.vpipeline.api.messaging.MessagingService;
import de.verdox.vpipeline.api.messaging.instruction.types.Ping;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class TicketTakeInstruction extends Ping {
    private final UUID ticketID;
    public TicketTakeInstruction(@NotNull UUID uuid, @NotNull UUID ticketID) {
        super(uuid);
        this.ticketID = ticketID;
    }

    @Override
    public void onPingReceive(MessagingService messagingService) {
        TicketPropagatorImpl ticketPropagator = (TicketPropagatorImpl) messagingService.getTicketPropagator();
        ticketPropagator.removeFromCache(ticketID);
    }
}
