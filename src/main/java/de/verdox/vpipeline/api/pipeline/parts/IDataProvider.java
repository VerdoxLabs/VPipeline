package de.verdox.vpipeline.api.pipeline.parts;

import com.google.gson.JsonElement;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 11:16
 */
public interface IDataProvider {
    JsonElement loadData(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID);
    boolean dataExist(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID);
    void save(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID, @NotNull JsonElement dataToSave);
    boolean remove(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID);
    Set<UUID> getSavedUUIDs(@NotNull Class<? extends IPipelineData> dataClass);
}