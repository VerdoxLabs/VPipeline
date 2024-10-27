package de.verdox.vpipeline.api.config;

import com.google.gson.JsonObject;
import de.verdox.vpipeline.api.NetworkParticipant;
import de.verdox.vpipeline.api.VNetwork;
import de.verdox.vserializer.util.gson.JsonUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

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

        if (overwrite)
            file.delete();

        if (!file.exists()) {
            file.createNewFile();
            JsonUtil.writeJsonObjectToFile(NetworkParticipant.SERIALIZER.toJson(defaultValue).getAsJsonObject(), file);
        }
    }

    public NetworkParticipant load() throws IOException {
        JsonObject jsonObject = JsonUtil.readJsonFromFile(file);
        return NetworkParticipant.SERIALIZER.fromJson(jsonObject);
    }

}
