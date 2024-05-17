package de.verdox.vpipeline.api.pipeline.parts;

import com.google.gson.JsonElement;
import de.verdox.vpipeline.api.modules.AttachedPipeline;
import de.verdox.vpipeline.api.pipeline.core.Pipeline;
import de.verdox.vpipeline.api.pipeline.core.SystemPart;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public interface DataProvider extends SystemPart {
    JsonElement loadData(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID);

    boolean dataExist(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID);

    void save(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID, @NotNull JsonElement dataToSave);

    boolean remove(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID);

    Set<UUID> getSavedUUIDs(@NotNull Class<? extends IPipelineData> dataClass);

    AttachedPipeline getAttachedPipeline();

    DataProviderLock getDataProviderLock();
}