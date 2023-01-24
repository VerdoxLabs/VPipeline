package de.verdox.vpipeline.impl.messaging.message;

import de.verdox.vpipeline.api.messaging.message.Message;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class MessageImpl implements Message {

    private final UUID sender;
    private final UUID instructionUUID;
    private final String senderIdentifier;
    private final int instructionID;
    private final List<String> parameters = new LinkedList<>();
    private final List<Object> dataToSend = new LinkedList<>();
    private final List<Object> response = new LinkedList<>();

    public MessageImpl(UUID sender, UUID instructionUUID, String senderIdentifier, int instructionID) {
        this.sender = sender;
        this.instructionUUID = instructionUUID;
        this.senderIdentifier = senderIdentifier;
        this.instructionID = instructionID;
    }

    @Override
    public @NotNull UUID getSender() {
        return sender;
    }

    @Override
    public int getInstructionID() {
        return instructionID;
    }

    @Override
    public @NotNull UUID getInstructionUUID() {
        return instructionUUID;
    }

    public MessageImpl addParameter(String parameter){
        this.parameters.add(parameter);
        return this;
    }

    public MessageImpl addParameters(List<String> parameters){
        this.parameters.addAll(parameters);
        return this;
    }

    public MessageImpl addDataToSend(Object dataToSend){
        this.dataToSend.add(dataToSend);
        return this;
    }

    public MessageImpl addDataToSend(List<Object> dataToSend){
        this.dataToSend.addAll(dataToSend);
        return this;
    }

    public MessageImpl addResponse(Object response){
        this.response.add(response);
        return this;
    }

    public MessageImpl addResponses(List<Object> responses){
        this.response.addAll(responses);
        return this;
    }

    @Override
    public @NotNull String getSenderIdentifier() {
        return senderIdentifier;
    }

    @Override
    public @NotNull List<String> getParameters() {
        return parameters;
    }

    @Override
    public @NotNull List<Object> dataToSend() {
        return dataToSend;
    }

    @Override
    public @NotNull List<Object> response() {
        return response;
    }
}
