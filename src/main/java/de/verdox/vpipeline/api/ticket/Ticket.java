package de.verdox.vpipeline.api.ticket;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Used to instruct nodes to do some sort of computation.
 * However, they are not meant to be exclusive to one {@link de.verdox.vpipeline.api.NetworkParticipant}.
 */
public interface Ticket extends Function<Object[], Ticket.TriState> {
    /**
     * Used to deserialize the input data of a ticket from a byte array.
     *
     * @param byteArrayDataInput the serialized input data
     */
    void readInputParameter(ByteArrayDataInput byteArrayDataInput);

    /**
     * Used to serialize the input data of a ticket into a byte array.
     *
     * @param byteArrayDataOutput the byte array data output to write to
     */
    void writeInputParameter(ByteArrayDataOutput byteArrayDataOutput);

    /**
     * Used to trigger a pipeline data preload that blocks the calling thread to ensure data availability during the ticket consumption.
     */
    void triggerDataPreloadBlocking();

    /**
     * The return type for tickets
     */
    enum TriState {
        /**
         * Returned if the ticket was completed successfully and should be removed from cache
         */
        TRUE,
        /**
         * Returned if the ticket was not completed successfully and should stay in cache
         */
        FALSE,
        /**
         * Returned if the ticket was not completed successfully and should be removed from cache
         */
        CANCELLED;

        public boolean evaluate(){
            return this.equals(TRUE) || this.equals(CANCELLED);
        }
    }
}
