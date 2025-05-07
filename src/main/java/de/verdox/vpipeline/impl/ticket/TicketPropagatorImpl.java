package de.verdox.vpipeline.impl.ticket;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.messaging.MessagingService;
import de.verdox.vpipeline.api.ticket.Ticket;
import de.verdox.vpipeline.api.ticket.TicketPropagator;
import net.bytebuddy.implementation.bytecode.Throw;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;

public class TicketPropagatorImpl implements TicketPropagator {
    private final Map<String, Class<? extends Ticket>> registeredTicketTypes = new HashMap<>();
    private final Map<String, Supplier<? extends Ticket>> constructorsOfTicketTypes = new HashMap<>();
    private final Map<Class<? extends Ticket>, String> ticketTypeToIdMapping = new HashMap<>();
    private final Map<Class<? extends Ticket>, Set<Ticket>> tickets = new ConcurrentHashMap<>();
    private final Map<UUID, Ticket> uuidToTicketMapping = new ConcurrentHashMap<>();
    private final Map<Ticket, UUID> ticketToUUIDMapping = new ConcurrentHashMap<>();
    private final MessagingService messagingService;

    public TicketPropagatorImpl(MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @Override
    public void issueTicket(Ticket ticket, UUID... receivers) {
        String type = ticketTypeToIdMapping.get(ticket.getClass());
        if (type == null)
            throw new IllegalArgumentException("Ticket type " + ticket.getClass().getName() + " not registered");
        ByteArrayDataOutput byteArrayDataOutput = ByteStreams.newDataOutput();

        byteArrayDataOutput.writeUTF(type);
        ticket.writeInputParameter(byteArrayDataOutput);
        UUID ticketUUID = UUID.randomUUID();
        this.messagingService.sendInstruction(new TicketIssuanceInstruction(ticketUUID, byteArrayDataOutput.toByteArray()), receivers);
        addTicketToCache(ticketUUID, ticket);
    }

    @Override
    public <T extends Ticket> Set<T> preloadDataAndConsumeTicket(Class<? extends T> ticketType, Object... inputParameters) {
        Set<T> set = new HashSet<>();
        if (!tickets.containsKey(ticketType))
            return set;
        tickets.get(ticketType).removeIf(ticket -> {
            try {
                ticket.triggerDataPreloadBlocking();
                return tryConsumeTicket(ticket, (Set<Ticket>) set, inputParameters);
            } catch (Throwable e) {
                LOGGER.log(Level.SEVERE, "Could not preload data and consume ticket " + ticket.getClass() + " (" + ticket + ")", e);
                return true;
            }
        });
        if (tickets.get(ticketType).isEmpty())
            tickets.remove(ticketType);
        return set;
    }

    @Override
    public <T extends Ticket> Set<T> consumeTicket(Class<? extends T> ticketType, Object... inputParameters) {
        Set<T> set = new HashSet<>();
        if (!tickets.containsKey(ticketType))
            return set;
        tickets.get(ticketType).removeIf(ticket -> tryConsumeTicket(ticket, (Set<Ticket>) set, inputParameters));
        if (tickets.get(ticketType).isEmpty())
            tickets.remove(ticketType);
        return set;
    }

    @Override
    public void triggerTicketDataPreload(Class<? extends Ticket> ticketType) {
        tickets.get(ticketType).parallelStream().forEach(Ticket::triggerDataPreloadBlocking);
    }

    @Override
    public <T extends Ticket> Set<T> preloadDataAndConsumeTicketGroup(Class<? extends T> ticketType, Object... inputParameters) {
        Set<T> set = new HashSet<>();
        tickets.forEach((aClass, tickets) -> {
            if (!ticketType.isAssignableFrom(aClass))
                return;
            tickets.removeIf(ticket -> {
                ticket.triggerDataPreloadBlocking();
                return tryConsumeTicket(ticket, (Set<Ticket>) set, inputParameters);
            });
        });
        return set;
    }

    @Override
    public <T extends Ticket> Set<T> consumeTicketGroup(Class<? extends T> ticketType, Object... inputParameters) {
        Set<T> set = new HashSet<>();
        tickets.forEach((aClass, tickets) -> {
            if (!ticketType.isAssignableFrom(aClass))
                return;
            tickets.removeIf(ticket -> tryConsumeTicket(ticket, (Set<Ticket>) set, inputParameters));
        });
        return set;
    }

    @Override
    public void triggerTicketDataPreloadGroup(Class<? extends Ticket> ticketType) {
        tickets.entrySet().stream().filter(classSetEntry -> ticketType.isAssignableFrom(classSetEntry.getKey())).parallel().flatMap(classSetEntry -> classSetEntry.getValue().stream()).forEach(ticket -> {
            try {
                ticket.triggerDataPreloadBlocking();
            } catch (Throwable e) {
                LOGGER.log(Level.SEVERE, "There was an error in the ticket " + ticket.getClass().getSimpleName() + "(" + ticket + ")", e);
            }
        });
    }

    @Override
    public <T extends Ticket> void registerTicketType(String id, Class<? extends T> ticketType, Supplier<T> constructor) {
        if (registeredTicketTypes.containsKey(id))
            throw new IllegalArgumentException("Ticket with id " + id + " already registered");
        registeredTicketTypes.put(id, ticketType);
        constructorsOfTicketTypes.put(id, constructor);
        ticketTypeToIdMapping.put(ticketType, id);
    }

    public void reconstructTicket(UUID ticketUUID, byte[] data) {
        ByteArrayDataInput in = ByteStreams.newDataInput(data);
        String ticketID = in.readUTF();

        Supplier<? extends Ticket> constructor = constructorsOfTicketTypes.get(ticketID);
        if (constructor == null)
            return;
        Ticket ticket = constructor.get();
        ticket.readInputParameter(in);
        addTicketToCache(ticketUUID, ticket);
    }

    private void addTicketToCache(UUID uuid, Ticket ticket) {
        tickets.computeIfAbsent(ticket.getClass(), aClass -> ConcurrentHashMap.newKeySet(100)).add(ticket);
        uuidToTicketMapping.put(uuid, ticket);
        ticketToUUIDMapping.put(ticket, uuid);
    }

    public void removeFromCache(UUID ticketUUID) {
        if (!uuidToTicketMapping.containsKey(ticketUUID))
            return;
        Ticket ticket = uuidToTicketMapping.remove(ticketUUID);
        ticketToUUIDMapping.remove(ticket);
        if (tickets.containsKey(ticket.getClass())) {
            tickets.get(ticket.getClass()).remove(ticket);
        }
    }

    private <T extends Ticket> boolean tryConsumeTicket(T ticket, Set<T> consumedSet, Object... inputParameters) {
        try {
            Ticket.TriState triState = ticket.apply(inputParameters);
            if (triState == null)
                return false;
            boolean value = triState.evaluate();
            if (value) {
                UUID ticketUUID = ticketToUUIDMapping.get(ticket);
                this.messagingService.sendInstruction(new TicketTakeInstruction(UUID.randomUUID(), ticketUUID));
                consumedSet.add(ticket);
            }
            return value;
        } catch (Throwable e) {
            LOGGER.log(Level.SEVERE, "Could not consume ticket " + ticket.getClass() + " (" + ticket + ")", e);
            return true;
        }
    }
}
