package de.verdox.vpipeline.api.pipeline.datatypes;

import com.google.gson.JsonParser;
import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.modules.AttachedPipeline;
import de.verdox.vpipeline.api.pipeline.core.Pipeline;
import de.verdox.vpipeline.api.pipeline.core.SystemPart;
import de.verdox.vpipeline.api.pipeline.parts.cache.local.DataAccess;
import de.verdox.vpipeline.api.util.AnnotationResolver;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public interface DataSynchronizer extends SystemPart {
    /**
     * Cleanup Function triggered when data is removed from cache
     */
    void cleanUp();

    /**
     * Pushes the local data to the Pipeline
     */
    default void pushUpdate(@NotNull IPipelineData data) {
        Objects.requireNonNull(data, "data can't be null!");
        if (!data.getClass().equals(getSynchronizingType()))
            throw new IllegalArgumentException("The type of input parameter data " + data.getClass() + " does not match the synchronizer type " + getSynchronizingType());
        int count = sendDataBlockToNetwork(new UpdateDataBlock(getSynchronizerUUID(), data.getObjectUUID(), getAttachedPipeline().getGson().toJson(data.serialize())));
        if (AnnotationResolver.getDataProperties(getSynchronizingType()).debugMode())
            NetworkLogger
                    .debug("Pushed network sync to " + count + " clients of " + data + " [" + data.getObjectUUID() + "]");
    }

    /**
     * Notifies other Servers that hold this data to delete it from local Cache
     */
    default void pushRemoval(@NotNull UUID uuid) {
        Objects.requireNonNull(uuid, "uuid can't be null!");
        int count = sendDataBlockToNetwork(new RemoveDataBlock(getSynchronizerUUID(), uuid));

        if (AnnotationResolver.getDataProperties(getSynchronizingType()).debugMode())
            NetworkLogger.debug("Pushed network removal to " + count + " of data " + getSynchronizingType().getSimpleName() + " [" + uuid + "]");
    }

    /**
     * Notifies other Servers that a data with this uuid was created, and they should load it to local cache
     */
    default void pushCreation(@NotNull IPipelineData data) {
        Objects.requireNonNull(data, "data can't be null!");
        int count = sendDataBlockToNetwork(new CreationDataBlock(getSynchronizerUUID(), data.getObjectUUID(), getAttachedPipeline()
                .getGson()
                .toJson(data.serialize())));
        if (AnnotationResolver.getDataProperties(getSynchronizingType()).debugMode())
            NetworkLogger
                    .debug("Pushed network creation to " + count + " of data " + data + " [" + data.getObjectUUID() + "]");
    }

    /**
     * Used to send a data block to the network
     *
     * @param dataBlock the data block
     * @return the amount of clients that received the synchronization
     */
    int sendDataBlockToNetwork(DataBlock dataBlock);

    /**
     * Gets the attached pipeline of this synchronizer instance
     *
     * @return the attached pipeline
     */
    AttachedPipeline getAttachedPipeline();

    /**
     * Gets the uuid of this synchronizer instance
     *
     * @return the uuid
     */
    UUID getSynchronizerUUID();

    /**
     * Gets the pipeline data type this synchronizer is used to synchronize
     *
     * @return The data type
     */
    Class<? extends IPipelineData> getSynchronizingType();

    abstract class DataBlock implements Serializable {
        protected final UUID senderUUID;
        protected final UUID dataUUID;

        public DataBlock(@NotNull UUID senderUUID, @NotNull UUID dataUUID) {
            Objects.requireNonNull(senderUUID);
            Objects.requireNonNull(dataUUID);
            this.senderUUID = senderUUID;
            this.dataUUID = dataUUID;
        }

        public UUID getSenderUUID() {
            return senderUUID;
        }

        public UUID getDataUUID() {
            return dataUUID;
        }

        public abstract void process(@NotNull Class<? extends IPipelineData> dataClass, @NotNull Pipeline pipeline);

        @Override
        public String toString() {
            return "DataBlock{" +
                    "senderUUID=" + senderUUID +
                    ", dataUUID=" + dataUUID +
                    '}';
        }
    }

    class RemoveDataBlock extends DataBlock {
        public RemoveDataBlock(@NotNull UUID senderUUID, @NotNull UUID dataUUID) {
            super(senderUUID, dataUUID);
        }

        @Override
        public void process(@NotNull Class<? extends IPipelineData> dataClass, @NotNull Pipeline pipeline) {
            IPipelineData remoteDataObject = pipeline.getLocalCache().loadObject(dataClass, dataUUID);
            if (remoteDataObject == null)
                return;

            if (AnnotationResolver.getDataProperties(dataClass).debugMode())
                NetworkLogger.debug("Received network removal for " + dataClass.getSimpleName() + " [" + remoteDataObject + " | " + dataClass + "]");
            if (!pipeline.getLocalCache().remove(dataClass, dataUUID))
                NetworkLogger
                        .getLogger()
                        .warning("Could not remove after network removal instruction [" + pipeline
                                .getLocalCache()
                                .dataExist(dataClass, dataUUID) + "]");
            pipeline.getLocalCache().remove(dataClass, dataUUID);

            //TODO: Maybe bauen wir einen proxy drum rum? In diesem Proxy können wir dann das Objekt abtöten evtl exception werfen oder ähnliches, wenn das Ding eigentlich nicht meh existiert.
            // Man sollte so oder so diese Objekte niemals local irgendwo anders cachen
        }
    }

    class CreationDataBlock extends DataBlock {
        private final String dataToUpdate;

        public CreationDataBlock(@NotNull UUID senderUUID, @NotNull UUID dataUUID, String dataToUpdate) {
            super(senderUUID, dataUUID);
            Objects.requireNonNull(dataToUpdate);
            this.dataToUpdate = dataToUpdate;
        }

        @Override
        public void process(@NotNull Class<? extends IPipelineData> dataClass, @NotNull Pipeline pipeline) {
            if (pipeline.getLocalCache().dataExist(dataClass, dataUUID)) {
                NetworkLogger
                        .getLogger()
                        .warning("Received data creation instruction for data that already exists in this nodes local cache [" + pipeline
                                .getLocalCache()
                                .dataExist(dataClass, dataUUID) + "]");
                return;
            }
            pipeline.getLocalCache().save(dataClass, dataUUID, JsonParser.parseString(dataToUpdate));
        }

        @Override
        public String toString() {
            return "CreationDataBlock{" +
                    "senderUUID=" + senderUUID +
                    ", dataUUID=" + dataUUID +
                    ", dataToUpdate='" + dataToUpdate + '\'' +
                    '}';
        }
    }

    class UpdateDataBlock extends DataBlock {
        private final String dataToUpdate;

        public UpdateDataBlock(@NotNull UUID senderUUID, @NotNull UUID dataUUID, String dataToUpdate) {
            super(senderUUID, dataUUID);
            Objects.requireNonNull(dataToUpdate);
            this.dataToUpdate = dataToUpdate;
        }

        @Override
        public void process(@NotNull Class<? extends IPipelineData> dataClass, @NotNull Pipeline pipeline) {
            // We save the data to local cache when there are data subscribers that wait for values
            if (pipeline.getLocalCache().hasDataSubscribers(dataClass, dataUUID)) {
                NetworkLogger.debug("Saving data due to existing subscribers for " + dataClass.getSimpleName() + " [" + dataUUID + "]");
                pipeline.getLocalCache().save(dataClass, dataUUID, JsonParser.parseString(dataToUpdate));
            }
            boolean dataExistsLocally = pipeline.getLocalCache().dataExist(dataClass, dataUUID);
            if (!dataExistsLocally)
                return;
            IPipelineData data = pipeline.getLocalCache().loadObject(dataClass, dataUUID);
            Objects.requireNonNull(data);
            if (AnnotationResolver.getDataProperties(dataClass).debugMode())
                NetworkLogger.debug("Received network sync for " + dataClass.getSimpleName() + " [" + data + " | " + dataUUID + "]");
            String dataBeforeSync = data.serialize().toString();
            data.deserialize(dataToUpdate);
            data.onSync(dataBeforeSync);
            DataAccess<IPipelineData> access = pipeline.getLocalCache().createAccess(dataClass, dataUUID);
            access.notifySubscribers(data);
        }

        @Override
        public String toString() {
            return "UpdateDataBlock{" +
                    "senderUUID=" + senderUUID +
                    ", dataUUID=" + dataUUID +
                    ", dataToUpdate='" + dataToUpdate + '\'' +
                    '}';
        }
    }
}
