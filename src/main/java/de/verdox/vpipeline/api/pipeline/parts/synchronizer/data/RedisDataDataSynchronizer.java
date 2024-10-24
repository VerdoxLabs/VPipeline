package de.verdox.vpipeline.api.pipeline.parts.synchronizer.data;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.modules.AttachedPipeline;
import de.verdox.vpipeline.api.pipeline.core.Pipeline;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import de.verdox.vpipeline.api.pipeline.datatypes.DataSynchronizer;
import de.verdox.vpipeline.api.util.AnnotationResolver;
import de.verdox.vpipeline.impl.util.RedisConnection;
import org.jetbrains.annotations.NotNull;
import org.redisson.api.RTopic;
import org.redisson.api.listener.MessageListener;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class RedisDataDataSynchronizer implements DataSynchronizer {
    private RTopic dataTopic;
    private MessageListener<DataBlock> messageListener;
    private final AttachedPipeline attachedPipeline;
    private final Class<? extends IPipelineData> dataClass;
    private final Pipeline pipeline;
    private final RedisConnection redisConnection;

    public RedisDataDataSynchronizer(@NotNull Class<? extends IPipelineData> dataClass, @NotNull Pipeline pipeline, @NotNull RedisConnection redisConnection) {
        this.dataClass = dataClass;
        this.pipeline = pipeline;

        Objects.requireNonNull(dataClass, "data can't be null!");
        Objects.requireNonNull(pipeline, "pipeline can't be null!");
        Objects.requireNonNull(redisConnection, "redisConnection can't be null!");
        this.redisConnection = redisConnection;
        this.attachedPipeline = new AttachedPipeline(GsonBuilder::create);
        this.attachedPipeline.attachPipeline(pipeline);
        connect();
    }

/*    @Deprecated
    private void oldListenerLogic(@NotNull Class<? extends IPipelineData> dataClass, @NotNull Pipeline pipeline, DataBlock dataBlock) {
        // Ignore own sent messages
        if (dataBlock.getSenderUUID().equals(senderUUID))
            return;
        IPipelineData remoteDataObject = pipeline.getLocalCache().loadObject(dataClass, dataBlock.getDataUUID());

        var uuid = dataBlock.getDataUUID();
        var serializedData = "";
        if (dataBlock instanceof RemoveDataBlock) {
            if (AnnotationResolver.getDataProperties(dataClass).debugMode())
                NetworkLogger.debug("Received network removal for " + dataClass.getSimpleName() + " [" + remoteDataObject + " | " + dataBlock.getDataUUID() + "]");
            if (!pipeline.getLocalCache().remove(dataClass, dataBlock.getDataUUID()))
                NetworkLogger
                        .getLogger()
                        .warning("Could not remove after network removal instruction [" + pipeline
                                .getLocalCache()
                                .dataExist(dataClass, uuid) + "]");
            return;
        } else if (dataBlock instanceof UpdateDataBlock updateDataBlock)
            serializedData = updateDataBlock.dataToUpdate;
        else if (dataBlock instanceof CreationDataBlock creationDataBlock)
            serializedData = creationDataBlock.dataToUpdate;

        if (remoteDataObject == null) {
            if (AnnotationResolver.getDataProperties(dataClass).debugMode())
                NetworkLogger.debug("Received network creation for " + dataClass.getSimpleName() + "[" + dataBlock.getDataUUID() + "]");
            this.pipeline
                    .getLocalCache()
                    .save(dataClass, uuid, JsonParser.parseString(serializedData));
        } else {
            if (AnnotationResolver.getDataProperties(dataClass).debugMode())
                NetworkLogger.debug("Received network sync for " + dataClass.getSimpleName() + " [" + remoteDataObject + " | " + dataBlock.dataUUID + "]");
            String dataBeforeSync = remoteDataObject.serialize().toString();
            remoteDataObject.deserialize(serializedData);
            remoteDataObject.onSync(dataBeforeSync);
        }
    }*/

    @Override
    public void cleanUp() {
        dataTopic.removeListener(messageListener);
    }

    @Override
    public int sendDataBlockToNetwork(DataBlock dataBlock) {
        return (int) dataTopic.publish(dataBlock);
    }

    @Override
    public AttachedPipeline getAttachedPipeline() {
        return attachedPipeline;
    }

    @Override
    public UUID getSynchronizerUUID() {
        return attachedPipeline.getAttachedPipeline().getNetworkParticipant().getUUID();
    }

    @Override
    public Class<? extends IPipelineData> getSynchronizingType() {
        return dataClass;
    }

    @Override
    public void shutdown() {
        disconnect();
    }

    @Override
    public void connect() {
        this.redisConnection.connect();
        this.dataTopic = redisConnection.getTopic(AnnotationResolver.getDataStorageClassifier(dataClass), dataClass);
        this.messageListener = (channel, dataBlock) -> {
            if (dataBlock.getSenderUUID().equals(pipeline.getNetworkParticipant().getUUID()))
                return;
            dataBlock.process(dataClass, pipeline);
            NetworkLogger.debug("["+pipeline.getNetworkParticipant().getUUID()+"] Received and processed dataBlock "+dataBlock);
        };
        dataTopic.addListener(DataBlock.class, messageListener);
        if (AnnotationResolver.getDataProperties(dataClass).debugMode())
            NetworkLogger.info("RedisDataSynchronizer started for " + dataClass.getSimpleName());
    }

    @Override
    public void disconnect() {
        this.redisConnection.disconnect();
    }
}
