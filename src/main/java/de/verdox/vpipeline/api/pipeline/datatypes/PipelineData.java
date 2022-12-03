package de.verdox.vpipeline.api.pipeline.datatypes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonElement;
import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.modules.AttachedPipeline;
import de.verdox.vpipeline.api.pipeline.SynchronizedAccess;
import de.verdox.vpipeline.api.pipeline.core.Pipeline;
import de.verdox.vpipeline.api.pipeline.core.PipelineSynchronizer;
import de.verdox.vpipeline.api.util.AnnotationResolver;
import de.verdox.vpipeline.api.pipeline.annotations.PipelineDataProperties;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 14:36
 */

@PipelineDataProperties
public abstract class PipelineData implements IPipelineData {

    private final UUID objectUUID;
    private transient final Synchronizer synchronizer;
    private transient final long cleanTime;
    private transient final TimeUnit cleanTimeUnit;
    private transient long lastUse = System.currentTimeMillis();
    private transient boolean markedForRemoval = false;
    private transient final AttachedPipeline attachedPipeline;

    public PipelineData(@NotNull Pipeline pipeline, @NotNull UUID objectUUID) {
        this.attachedPipeline = new AttachedPipeline(gsonBuilder -> gsonBuilder
                .setPrettyPrinting()
                .serializeNulls()
                .registerTypeAdapter(getClass(), (InstanceCreator<PipelineData>) type -> this)
                .create());
        this.attachedPipeline.attachPipeline(pipeline);
        Objects.requireNonNull(pipeline, "pipeline can't be null!");
        Objects.requireNonNull(objectUUID, "objectUUID can't be null!");

        this.objectUUID = objectUUID;
        if (pipeline.getSynchronizingService() != null)
            this.synchronizer = pipeline.getSynchronizingService().getSynchronizer(pipeline, this);
        else
            this.synchronizer = new Synchronizer() {
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

    @Override
    public UUID getObjectUUID() {
        return objectUUID;
    }

    @Override
    public JsonElement serialize() {
        synchronized (this) {
            markedForRemoval = false;
            return attachedPipeline.getGson().toJsonTree(this);
        }
    }

    @Override
    public String deserialize(JsonElement jsonObject) {
        synchronized (this) {
            markedForRemoval = false;
            attachedPipeline.getGson().fromJson(jsonObject, getClass());
            return attachedPipeline.getGson().toJson(jsonObject);
        }
    }

    @Override
    public Synchronizer getSynchronizer() {
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
    public CompletableFuture<Boolean> save(boolean saveToStorage) {
        synchronized (this) {
            var future = new CompletableFuture<Boolean>();
            updateLastUsage();
            //TODO: Completable Future
            if (this.synchronizer == null) {
                attachedPipeline.getAttachedPipeline()
                                .getPipelineSynchronizer()
                                .synchronize(PipelineSynchronizer.DataSourceType.LOCAL, PipelineSynchronizer.DataSourceType.GLOBAL_STORAGE, getClass(), getObjectUUID(), () -> {
                                    future.complete(true);
                                });
            } else {
                this.synchronizer.pushUpdate(this, () -> {
                    if (!saveToStorage) {
                        future.complete(true);
                        return;
                    }
                    attachedPipeline.getAttachedPipeline()
                                    .getPipelineSynchronizer()
                                    .synchronize(PipelineSynchronizer.DataSourceType.LOCAL, PipelineSynchronizer.DataSourceType.GLOBAL_STORAGE, getClass(), getObjectUUID(), () -> {
                                        future.complete(true);
                                    });
                });
            }
            return future;
        }
    }

    public static <S extends IPipelineData> S instantiateData(@NotNull Pipeline pipeline, @NotNull Class<? extends S> dataClass, @NotNull UUID objectUUID) {
        try {
            S dataObject = dataClass
                    .getDeclaredConstructor(Pipeline.class, UUID.class)
                    .newInstance(pipeline, objectUUID);
            dataObject.updateLastUsage();
            return dataClass.cast(dataObject);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public AttachedPipeline getAttachedPipeline() {
        return attachedPipeline;
    }
}
