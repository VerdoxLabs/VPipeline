package de.verdox.vpipeline.api.pipeline.datatypes;

import com.google.gson.InstanceCreator;
import com.google.gson.JsonElement;
import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.modules.AttachedPipeline;
import de.verdox.vpipeline.api.pipeline.annotations.PipelineDataProperties;
import de.verdox.vpipeline.api.pipeline.core.Pipeline;
import de.verdox.vpipeline.api.util.AnnotationResolver;
import de.verdox.vpipeline.impl.util.CallbackUtil;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@PipelineDataProperties
public abstract class PipelineData implements IPipelineData {

    private final UUID objectUUID;
    private transient final Synchronizer synchronizer;
    private transient final long cleanTime;
    private transient final TimeUnit cleanTimeUnit;
    private transient long lastUse = System.currentTimeMillis();
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
            this.synchronizer = pipeline.getSynchronizingService().getOrCreate(pipeline, this);
        else
            this.synchronizer = new DummyDataSynchronizer();
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
        return attachedPipeline.getGson().toJsonTree(this);
    }

    @Override
    public String deserialize(JsonElement jsonObject) {
        if (AnnotationResolver.getDataProperties(getClass()).debugMode())
            NetworkLogger.debug("Updating " + this);
        var data = attachedPipeline.getGson().fromJson(jsonObject, getClass());
        return attachedPipeline.getGson().toJson(jsonObject);
    }

    @Override
    public String deserialize(String jsonString) {
        if (AnnotationResolver.getDataProperties(getClass()).debugMode())
            NetworkLogger.debug("Updating " + this);
        attachedPipeline.getGson().fromJson(jsonString, getClass());
        return attachedPipeline.getGson().toJson(jsonString);
    }

    @Override
    public @NotNull Synchronizer getSynchronizer() {
        return synchronizer;
    }

    @Override
    public void updateLastUsage() {
        lastUse = System.currentTimeMillis();
    }

    @Override
    public CompletableFuture<Void> save(boolean saveToStorage) {
        updateLastUsage();
        //TODO: Completable Future
        return attachedPipeline.getAttachedPipeline().getPipelineSynchronizer().sync(this, saveToStorage);
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

    static class DummyDataSynchronizer implements Synchronizer {

        @Override
        public void shutdown() {

        }

        @Override
        public void cleanUp() {

        }

        @Override
        public void pushUpdate(@NotNull IPipelineData data, Runnable callback) {
            NetworkLogger.getLogger()
                         .warning("[" + data.getAttachedPipeline().getAttachedPipeline().getNetworkParticipant()
                                            .getIdentifier() + "] Syncing with dummy data synchronizer");
            CallbackUtil.runIfNotNull(callback);
        }

        @Override
        public void pushRemoval(@NotNull UUID uuid, Runnable callback) {
            CallbackUtil.runIfNotNull(callback);
        }

        @Override
        public void pushCreation(@NotNull IPipelineData data, Runnable callback) {

        }
    }
}
