package de.verdox.vpipeline.api.messaging.instruction;

import de.verdox.vpipeline.api.messaging.MessagingService;
import de.verdox.vpipeline.impl.messaging.ResponseCollectorImpl;

import java.util.UUID;


public interface Instruction<R> {

    UUID getUuid();
    int getInstructionID();
    long getCreationTimeStamp();
    boolean isResponse();
    UUID getSenderUUID();
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

    ResponseCollector<R> getResponseCollector();
}

