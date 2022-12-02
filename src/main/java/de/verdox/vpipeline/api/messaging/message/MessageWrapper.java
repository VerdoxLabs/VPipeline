package de.verdox.vpipeline.api.messaging.message;

import de.verdox.vpipeline.api.messaging.MessagingService;

import java.util.UUID;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 23:01
 */
public record MessageWrapper(Message message) {

    public boolean isInstruction() {
        return parameterContains(MessagingService.INSTRUCTION_IDENTIFIER);
    }

    public boolean isResponse() {
        return parameterContains(MessagingService.RESPONSE_IDENTIFIER);
    }

    public UUID getSenderUUID() {
        return message.getData(0, UUID.class);
    }

    public int getInstructionID() {
        return message.getData(1, Integer.class);
    }

    public UUID getInstructionUUID() {
        return message.getData(2, UUID.class);
    }

    public String[] getParameters() {
        return message.getData(3, String[].class);
    }

    public Object[] getData() {
        return message.getData(4, Object[].class);
    }

    public Object[] getResponseData() {
        if(!isResponse())
            throw new RuntimeException("Message is not a response");
        return message.getData(5, Object[].class);
    }

    public boolean validate(Class<?>... types) {
        if (message.size() != types.length)
            return false;

        for (int i = 0; i < types.length; i++) {
            Class<?> type = types[i];
            if (!message.isTypeOf(i, type))
                return false;
        }
        return true;
    }

    public boolean parameterContains(String... parameters) {
        if (message.getParameters() == null)
            return false;
        for (int i = 0; i < message.getParameters().length; i++) {
            String messageParameter = message.getParameters()[i];
            if (i >= parameters.length)
                continue;
            String neededParameter = parameters[i];
            if (!messageParameter.equals(neededParameter))
                return false;
        }
        return true;
    }
}
