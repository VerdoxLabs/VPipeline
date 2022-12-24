package de.verdox.vpipeline.api.network;

import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public interface RemoteParticipant extends IPipelineData {
    void saveCachedData(String key, Object value);

    <T> T getData(Class<? extends T> type, String key);

    String getIdentifier();

    static UUID getParticipantUUID(String identifier) {
        return UUID.nameUUIDFromBytes(identifier.getBytes(StandardCharsets.UTF_8));
    }
}
