package de.verdox.vpipeline.api.pipeline.parts.cache.global;

import com.google.gson.JsonElement;
import de.verdox.vpipeline.api.modules.AttachedPipeline;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import de.verdox.vpipeline.api.pipeline.parts.GlobalCache;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;

public class DummyGlobalCache implements GlobalCache {
    @Override
    public void connect() {

    }

    @Override
    public void disconnect() {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public JsonElement loadData(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID) {
        return null;
    }

    @Override
    public boolean dataExist(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID) {
        return false;
    }

    @Override
    public void save(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID, @NotNull JsonElement dataToSave) {

    }

    @Override
    public boolean remove(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID) {
        return false;
    }

    @Override
    public Set<UUID> getSavedUUIDs(@NotNull Class<? extends IPipelineData> dataClass) {
        return null;
    }

    @Override
    public AttachedPipeline getAttachedPipeline() {
        return null;
    }
}
