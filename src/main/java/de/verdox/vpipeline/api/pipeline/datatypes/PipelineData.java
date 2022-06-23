package de.verdox.vpipeline.api.pipeline.datatypes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonElement;
import de.verdox.vpipeline.api.pipeline.core.IPipeline;
import de.verdox.vpipeline.api.pipeline.core.IPipelineSynchronizer;
import de.verdox.vpipeline.api.util.AnnotationResolver;
import de.verdox.vpipeline.api.pipeline.annotations.PipelineDataProperties;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 14:36
 */
public abstract class PipelineData implements IPipelineData {

    private final UUID objectUUID;

    private transient final ISynchronizer synchronizer;
    private transient final Gson gson;
    private transient final long cleanTime;
    private transient final TimeUnit cleanTimeUnit;
    private transient long lastUse = System.currentTimeMillis();
    private transient boolean markedForRemoval = false;
    private transient final IPipeline pipeline;

    public PipelineData(@NotNull IPipeline pipeline, @NotNull UUID objectUUID) {
        this.pipeline = pipeline;
        Objects.requireNonNull(pipeline, "pipeline can't be null!");
        Objects.requireNonNull(objectUUID, "objectUUID can't be null!");

        this.objectUUID = objectUUID;
        this.gson = new GsonBuilder().setPrettyPrinting().serializeNulls().registerTypeAdapter(getClass(), (InstanceCreator<PipelineData>) type -> this)
                .create();
        if (pipeline.getSynchronizingService() != null)
            this.synchronizer = pipeline.getSynchronizingService().getSynchronizer(pipeline, this);
        else
            this.synchronizer = new ISynchronizer() {
                @Override
                public void cleanUp() {

                }

                @Override
                public void pushUpdate(IPipelineData pipelineData, Runnable callback) {

                }

                @Override
                public void pushRemoval(IPipelineData pipelineData, Runnable callback) {

                }

                @Override
                public void shutdown() {

                }
            };
        PipelineDataProperties dataProperties = AnnotationResolver.getDataProperties(getClass());
        this.cleanTime = dataProperties.time();
        this.cleanTimeUnit = dataProperties.timeUnit();
    }

    //TODO: Implement
    @Override
    public UUID getObjectUUID() {
        return objectUUID;
    }

    @Override
    public JsonElement serialize() {
        markedForRemoval = false;
        return gson.toJsonTree(this);
    }

    @Override
    public String deserialize(JsonElement jsonObject) {
        markedForRemoval = false;
        gson.fromJson(jsonObject, getClass());
        return gson.toJson(jsonObject);
    }

    @Override
    public ISynchronizer getSynchronizer() {
        return synchronizer;
    }

    @Override
    public void markRemoval() {
        markedForRemoval = true;
    }

    @Override
    public boolean isMarkedForRemoval() {
        return markedForRemoval;
    }

    @Override
    public void updateLastUsage() {
        lastUse = System.currentTimeMillis();
    }

    @Override
    public void save(boolean saveToStorage) {
        updateLastUsage();
        if (this.synchronizer == null) {
            pipeline.getPipelineSynchronizer().synchronize(IPipelineSynchronizer.DataSourceType.LOCAL, IPipelineSynchronizer.DataSourceType.GLOBAL_STORAGE, getClass(), getObjectUUID());
            return;
        }
        this.synchronizer.pushUpdate(this, () -> {
            if (!saveToStorage)
                return;
            pipeline.getPipelineSynchronizer().synchronize(IPipelineSynchronizer.DataSourceType.LOCAL, IPipelineSynchronizer.DataSourceType.GLOBAL_STORAGE, getClass(), getObjectUUID());
        });
    }
}
