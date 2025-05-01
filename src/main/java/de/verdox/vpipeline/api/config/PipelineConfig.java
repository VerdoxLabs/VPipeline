package de.verdox.vpipeline.api.config;

import de.verdox.vpipeline.api.NetworkParticipant;
import de.verdox.vpipeline.api.VNetwork;
import de.verdox.vserializer.generic.SerializationContext;
import de.verdox.vserializer.generic.SerializationElement;
import de.verdox.vserializer.json.JsonSerializerContext;

import java.io.File;
import java.io.IOException;

public class PipelineConfig {
    private final File file;

    public PipelineConfig(File file) throws IOException {
        this(file, VNetwork.getConstructionService().createNetworkParticipant()
                .withName("pipeline")
                .withPipeline()
                .withMessagingService()
                .build());
    }

    public PipelineConfig(File file, NetworkParticipant defaultValue) throws IOException {
        this(file, defaultValue, false);
    }

    public PipelineConfig(File file, NetworkParticipant defaultValue, boolean overwrite) throws IOException {
        this.file = file;

        file.getParentFile().mkdirs();

        if (overwrite) {
            file.delete();
        }

        if (!file.exists()) {
            SerializationContext context = new JsonSerializerContext();
            context.writeToFile(NetworkParticipant.SERIALIZER.serialize(context, defaultValue), file);
        }
    }

    public NetworkParticipant load() throws IOException {
        SerializationElement element = new JsonSerializerContext().readFromFile(file);
        return NetworkParticipant.SERIALIZER.deserialize(element);
    }

}
