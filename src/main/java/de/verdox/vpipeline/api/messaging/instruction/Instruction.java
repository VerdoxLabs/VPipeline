package de.verdox.vpipeline.api.messaging.instruction;

import de.verdox.vpipeline.api.messaging.MessagingService;
import de.verdox.vpipeline.impl.messaging.ResponseCollectorImpl;

import java.util.UUID;


public interface Instruction<R> {

    /**
     * Returns the UUID of this instruction
     * @return the uuid
     */
    UUID getUuid();

    /**
     * Returns the unique id of this instruction type
     * @return the id
     */
    int getInstructionID();

    /**
     * Returns the timestamp when the instruction was created
     * @return the timestamp
     */
    long getCreationTimeStamp();

    /**
     * Returns whether the instruction awaits is a response to another instruction
     * @return true if it is a response
     */
    boolean isResponse();

    /**
     * Returns the uuid of the sender. This uuid corresponds to the uuid of the messaging service
     * @return the uuid
     */
    UUID getSenderUUID();

    /**
     * Returns the identifier of the sender. This corresponds to the id of the network participant this message was sent from.
     * @return the identifier
     */
    String getSenderIdentifier();

    /**
     * @param receiversAmount Amount of clients that will receive the instruction
     * @return True if the instruction should be sent to the network
     */
    boolean onSend(MessagingService messagingService, long receiversAmount);

    /**
     * Defines what happens if a network client receives this instruction.
     *
     * @return The defined return value
     */
    R onInstructionReceive(MessagingService messagingService);

    /**
     * Defines what happens if a network client receives a response to its sent instruction
     * @param response The response the client has received.
     */
    void onResponseReceive(MessagingService messagingService, R response);

    /**
     * Returns the response collector of this instruction
     * @return the response collector
     */
    ResponseCollector<R> getResponseCollector();
}

