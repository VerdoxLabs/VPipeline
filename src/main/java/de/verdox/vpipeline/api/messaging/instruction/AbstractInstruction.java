package de.verdox.vpipeline.api.messaging.instruction;

import java.util.UUID;

public abstract class AbstractInstruction<R> implements Instruction<R> {
    private final UUID uuid;
    private UUID senderUUID;
    private String senderIdentifier;
    private int registeredInstructionID;
    private boolean isResponse;
    private R responseToSend;
    private transient ResponseCollector<R> responseCollector;
    private final long creationTimeStamp = System.currentTimeMillis();

    public AbstractInstruction(UUID uuid) {
        this.uuid = uuid;
    }

    public void setupInstruction(int registeredInstructionID, UUID sender, String senderIdentifier) {
        this.registeredInstructionID = registeredInstructionID;
        this.senderUUID = sender;
        this.senderIdentifier = senderIdentifier;
    }

    public UUID getUuid() {
        return uuid;
    }

    @Override
    public int getInstructionID() {
        return registeredInstructionID;
    }

    @Override
    public long getCreationTimeStamp() {
        return creationTimeStamp;
    }

    public String getSenderIdentifier() {
        return senderIdentifier;
    }

    @Override
    public ResponseCollector<R> getResponseCollector() {
        return responseCollector;
    }

    public void setResponseCollector(ResponseCollector<R> responseCollector) {
        this.responseCollector = responseCollector;
    }

    public UUID getSenderUUID() {
        return senderUUID;
    }

    public boolean isResponse() {
        return isResponse;
    }

    public R getResponseToSend() {
        return responseToSend;
    }

    public void setResponseToSend(R responseToSend) {
        this.responseToSend = responseToSend;
        this.isResponse = true;
    }
}
