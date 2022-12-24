package de.verdox.vpipeline.impl.network;

import de.verdox.vpipeline.api.network.RemoteParticipant;
import de.verdox.vpipeline.api.pipeline.annotations.DataStorageIdentifier;
import de.verdox.vpipeline.api.pipeline.annotations.PipelineDataProperties;
import de.verdox.vpipeline.api.pipeline.core.Pipeline;
import de.verdox.vpipeline.api.pipeline.datatypes.PipelineData;
import de.verdox.vpipeline.api.pipeline.enums.DataContext;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@PipelineDataProperties(cleanOnNoUse = true, time = 6, timeUnit = TimeUnit.SECONDS, dataContext = DataContext.CACHE_ONLY)
@DataStorageIdentifier(identifier = "network.RemoteParticipant")
public class RemoteParticipantImpl extends PipelineData implements RemoteParticipant {
    private String identifier;
    private Map<String, Object> cachedData = new ConcurrentHashMap<>();

    public RemoteParticipantImpl(@NotNull Pipeline pipeline, @NotNull UUID objectUUID) {
        super(pipeline, objectUUID);
    }

    RemoteParticipantImpl setIdentifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public void saveCachedData(String key, Object value) {
        cachedData.put(key, value);
    }

    @Override
    public <T> T getData(Class<? extends T> type, String key) {
        return type.cast(cachedData.getOrDefault(key, null));
    }
}
