package de.verdox.vpipeline.api.ticket;

import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Used to send {@link Ticket}s to the network.
 * They are used to instruct nodes to do some sort of computation.
 */

public interface TicketPropagator {
    /**
     * Used to issue a new ticket to the network.
     *
     * @param ticket the ticket
     * @param receivers the network participants to receive the ticket. If the array is empty it is published to everyone
     */
    void issueTicket(Ticket ticket, UUID... receivers);

    /**
     * Used locally to preload ticket data and trigger tickets of a specific type
     * The programmer may specify on their own when data preload and ticket consumption happens by calling this function.
     *
     * @param ticketType      the ticket type
     * @param inputParameters the input parameters for this ticket
     */
    <T extends Ticket> Set<T> preloadDataAndConsumeTicket(Class<? extends T> ticketType, Object... inputParameters);

    /**
     * Used locally to trigger tickets of a specific type
     * The programmer may specify on their own when data preload happens by calling this function.
     *
     * @param ticketType      the ticket type
     * @param inputParameters the input parameters for this ticket
     */
    <T extends Ticket> Set<T> consumeTicket(Class<? extends T> ticketType, Object... inputParameters);

    /**
     * Some {@link Ticket}s need to load data before they can be run. In some cases it is advantageous to not load data in a {@link Ticket}.
     * The programmer may specify on their own when data preload happens by calling this function.
     *
     * @param ticketType      the ticket type
     */
    void triggerTicketDataPreload(Class<? extends Ticket> ticketType);

    /**
     * Used locally to preload ticket groups data and trigger tickets of a specific type.
     * A ticket group consists of tickets that derive have the same type as the provided ticketType or are a subclass.
     * The programmer may specify on their own when data preload and ticket consumption happens by calling this function.
     *
     * @param ticketType      the ticket type
     * @param inputParameters the input parameters for this ticket
     */
    <T extends Ticket> Set<T> preloadDataAndConsumeTicketGroup(Class<? extends T> ticketType, Object... inputParameters);

    /**
     * Used locally to trigger tickets of a specific type.
     * A ticket group consists of tickets that derive have the same type as the provided ticketType or are a subclass.
     * The programmer may specify on their own when data preload happens by calling this function.
     *
     * @param ticketType      the ticket type
     * @param inputParameters the input parameters for this ticket
     */
    <T extends Ticket> Set<T> consumeTicketGroup(Class<? extends T> ticketType, Object... inputParameters);

    /**
     * Some {@link Ticket}s need to load data before they can be run. In some cases it is advantageous to not load data in a {@link Ticket}.
     * A ticket group consists of tickets that derive have the same type as the provided ticketType or are a subclass.
     * The programmer may specify on their own when data preload happens by calling this function.
     *
     * @param ticketType      the ticket type
     */
    void triggerTicketDataPreloadGroup(Class<? extends Ticket> ticketType);

    /**
     * Used to register a new ticket type to the {@link TicketPropagator}.
     *
     * @param id          the unique id of the ticket type
     * @param ticketType  the type
     * @param constructor the ticket constructor
     * @param <T>         the generic ticket type
     */
    <T extends Ticket> void registerTicketType(String id, Class<? extends T> ticketType, Supplier<T> constructor);
}
