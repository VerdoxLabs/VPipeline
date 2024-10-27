package de.verdox.vpipeline.api.pipeline.parts.synchronizer.data;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
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
    private MessageListener<String> messageListener;
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

    @Override
    public void cleanUp() {
        dataTopic.removeListener(messageListener);
    }

    @Override
    public int sendDataBlockToNetwork(DataBlock dataBlock) {
        String serializedDataBlock = DataSynchronizer.DATA_BLOCK_SERIALIZER.toJson(dataBlock).toString();
        return (int) dataTopic.publish(serializedDataBlock);
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
        this.messageListener = (channel, jsonString) -> {
            JsonElement jsonElement = JsonParser.parseString(jsonString);
            DataBlock dataBlock = DataSynchronizer.DATA_BLOCK_SERIALIZER.fromJson(jsonElement);

            if (dataBlock.getSenderUUID().equals(pipeline.getNetworkParticipant().getUUID()))
                return;
            dataBlock.process(dataClass, pipeline);
            NetworkLogger.debug("["+pipeline.getNetworkParticipant().getUUID()+"] Received and processed dataBlock "+dataBlock);
        };
        dataTopic.addListener(String.class, messageListener);
        if (AnnotationResolver.getDataProperties(dataClass).debugMode())
            NetworkLogger.info("RedisDataSynchronizer started for " + dataClass.getSimpleName());
    }

    @Override
    public void disconnect() {
        this.redisConnection.disconnect();
    }
}
