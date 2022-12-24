package de.verdox.vpipeline.impl.messaging.message;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import de.verdox.vpipeline.api.messaging.message.Message;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.UUID;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 19.06.2022 12:48
 */
public record SimpleMessage(UUID sender, String senderIdentifier,
                            String[] parameters, Object[] dataToSend) implements Message {
    @Override
    public @NotNull UUID getSender() {
        return sender;
    }

    @Override
    public @NotNull String getSenderIdentifier() {
        return senderIdentifier;
    }

    @Override
    public String[] getParameters() {
        return parameters;
    }

    @Override
    public Object[] dataToSend() {
        return dataToSend;
    }
}
