package de.verdox.vpipeline.api.pipeline.datatypes;

import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.modules.AttachedPipeline;
import de.verdox.vpipeline.api.pipeline.annotations.PipelineDataProperties;
import de.verdox.vpipeline.api.pipeline.core.Pipeline;
import de.verdox.vpipeline.api.util.AnnotationResolver;
import de.verdox.vserializer.generic.Serializer;
import de.verdox.vserializer.json.JsonSerializationElement;
import de.verdox.vserializer.json.JsonSerializerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@PipelineDataProperties
public abstract class PipelineData implements IPipelineData {
    private final UUID objectUUID;
    private transient final DataSynchronizer dataSynchronizer;
    private transient final long cleanTime;
    private transient final TimeUnit cleanTimeUnit;
    private transient long lastUse = System.currentTimeMillis();
    private transient final AttachedPipeline attachedPipeline;
    @Nullable
    private transient Serializer<IPipelineData> customSerializer;

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
            this.dataSynchronizer = pipeline.getSynchronizingService().getOrCreate(pipeline, this);
        else
            this.dataSynchronizer = new DummyDataDataSynchronizer(pipeline, getClass());
        PipelineDataProperties dataProperties = AnnotationResolver.getDataProperties(getClass());
        this.cleanTime = dataProperties.time();
        this.cleanTimeUnit = dataProperties.timeUnit();
    }

    private void searchForCustomSerializer() {
        Serializer<? extends IPipelineData> customJsonSerializer = getCustomSerializer();
        if (customJsonSerializer != null) {
            if (!customJsonSerializer.getType().equals(getClass()))
                throw new IllegalStateException("The provided custom json serializer for the pipeline data class " + getClass().getName() + " does only accept objects of type " + customJsonSerializer.getType() + ". Please make sure that these types match!");
            this.customSerializer = (Serializer<IPipelineData>) customJsonSerializer;
        }
    }

    @Override
    public UUID getObjectUUID() {
        return objectUUID;
    }

    @Override
    public JsonElement serialize() {
        try {
            if (this.customSerializer != null) {
                return ((JsonSerializationElement) customSerializer.serialize(new JsonSerializerContext(), this)).getJsonElement();
            }
            return attachedPipeline.getGson().toJsonTree(this);
        } catch (Throwable e) {
            NetworkLogger.getLogger().log(Level.WARNING, "Error while serializing " + getObjectUUID() + " | " + getClass().getSimpleName(), e);
            return new JsonObject();
        }
    }

    @Override
    public void deserialize(JsonElement jsonObject) {
        try {
            if (AnnotationResolver.getDataProperties(getClass()).debugMode())
                NetworkLogger.debug("Updating " + this);
            if (this.customSerializer != null) {
                customSerializer.updateLiveObjectFromJson(this, new JsonSerializerContext().toElement(jsonObject));
            }
            else {
                attachedPipeline.getGson().fromJson(jsonObject, getClass());
            }
        } catch (Throwable e) {
            NetworkLogger.getLogger().log(Level.WARNING, "Error while deserializing " + getObjectUUID() + " | " + getClass().getSimpleName(), e);
        }
    }

    @Override
    public @NotNull DataSynchronizer getSynchronizer() {
        return dataSynchronizer;
    }

    @Override
    public void updateLastUsage() {
        lastUse = System.currentTimeMillis();
    }

    @Override
    public void save(boolean saveToStorage) {
        updateLastUsage();
        attachedPipeline.getAttachedPipeline().getPipelineSynchronizer().sync(this, saveToStorage);
    }

    public static <S extends IPipelineData> S instantiateData(@NotNull Pipeline pipeline, @NotNull Class<? extends S> dataClass, @NotNull UUID objectUUID) {
        try {
            S dataObject = dataClass
                    .getDeclaredConstructor(Pipeline.class, UUID.class)
                    .newInstance(pipeline, objectUUID);
            if (dataObject instanceof PipelineData pipelineData) {
                pipelineData.searchForCustomSerializer();
            }
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

    public static class DummyDataDataSynchronizer implements DataSynchronizer {
        private final AttachedPipeline attachedPipeline = new AttachedPipeline(GsonBuilder::create);
        private final Class<? extends IPipelineData> type;

        public DummyDataDataSynchronizer(@NotNull Pipeline pipeline, @NotNull Class<? extends IPipelineData> type) {
            this.type = type;
            attachedPipeline.attachPipeline(pipeline);
        }

        @Override
        public void shutdown() {

        }

        @Override
        public void cleanUp() {

        }

        @Override
        public void pushUpdate(@NotNull IPipelineData data) {
            NetworkLogger.debug("[" + data.getAttachedPipeline().getAttachedPipeline().getNetworkParticipant().getIdentifier() + "] Syncing with dummy data synchronizer");
        }

        @Override
        public int sendDataBlockToNetwork(DataBlock dataBlock) {
            return 0;
        }

        @Override
        public AttachedPipeline getAttachedPipeline() {
            return attachedPipeline;
        }

        @Override
        public UUID getSynchronizerUUID() {
            return UUID.randomUUID();
        }

        @Override
        public Class<? extends IPipelineData> getSynchronizingType() {
            return type;
        }

        @Override
        public void connect() {

        }

        @Override
        public void disconnect() {

        }
    }
}
