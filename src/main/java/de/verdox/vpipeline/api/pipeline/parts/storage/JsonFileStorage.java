package de.verdox.vpipeline.api.pipeline.parts.storage;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import de.verdox.vserializer.SerializableField;
import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.modules.AttachedPipeline;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import de.verdox.vpipeline.api.pipeline.parts.GlobalStorage;
import de.verdox.vserializer.generic.Serializer;
import de.verdox.vserializer.generic.SerializerBuilder;
import jodd.io.FileNameUtil;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class JsonFileStorage implements GlobalStorage {
    public static final Serializer<JsonFileStorage> SERIALIZER = SerializerBuilder.create("json_file_storage", JsonFileStorage.class)
            .constructor(
                    new SerializableField<>("path", Serializer.Primitive.STRING, jsonFileStorage -> jsonFileStorage.path.toString()),
                    s -> new JsonFileStorage(Path.of(s))
            )
            .build();

    private final Path path;
    private final AttachedPipeline attachedPipeline;

    public JsonFileStorage(Path path) {
        this.path = path;
        this.attachedPipeline = new AttachedPipeline(GsonBuilder::create);
    }

    @Override
    public JsonElement loadData(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID) {
        Objects.requireNonNull(dataClass, "dataClass can't be null!");
        Objects.requireNonNull(objectUUID, "objectUUID can't be null!");

        try {
            return loadFromFile(dataClass, objectUUID);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean dataExist(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID) {
        Objects.requireNonNull(dataClass, "dataClass can't be null!");
        Objects.requireNonNull(objectUUID, "objectUUID can't be null!");
        return Files.exists(getFilePath(dataClass, objectUUID));
    }

    @Override
    public void save(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID, @NotNull JsonElement dataToSave) {
        Objects.requireNonNull(dataClass, "dataClass can't be null!");
        Objects.requireNonNull(objectUUID, "objectUUID can't be null!");
        try {
            saveJsonToFile(dataClass, objectUUID, dataToSave);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean remove(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID) {
        Objects.requireNonNull(dataClass, "dataClass can't be null!");
        Objects.requireNonNull(objectUUID, "objectUUID can't be null!");
        if (!dataExist(dataClass, objectUUID))
            return false;
        try {
            Files.deleteIfExists(getFilePath(dataClass, objectUUID));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Set<UUID> getSavedUUIDs(@NotNull Class<? extends IPipelineData> dataClass) {
        Objects.requireNonNull(dataClass, "dataClass can't be null!");
        Path parentFolder = getParentFolder(dataClass);
        if (!parentFolder.toFile().exists())
            return Set.of();
        try (var stream = Files.walk(parentFolder, 1)) {
            return stream
                    .skip(1)
                    .filter(path1 -> FileNameUtil.getExtension(path1.getFileName().toString()).equals(".json"))
                    .map(path1 -> FileNameUtil.getBaseName(path1.toString()))
                    .map(UUID::fromString)
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            e.printStackTrace();
            return Set.of();
        }
    }

    @Override
    public AttachedPipeline getAttachedPipeline() {
        return attachedPipeline;
    }

    private void saveJsonToFile(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID, @NotNull JsonElement dataToSave) throws IOException {
        if (dataToSave.isJsonNull())
            return;
        Path path = getFilePath(dataClass, objectUUID);

        File file = new File(path.toUri());
        if (!file.exists()) {
            if(!file.getParentFile().mkdirs() && !file.getParentFile().exists())
                throw new RuntimeException("Could not create folder structure JsonFileStorage [" + path + "]");
            else if (!file.createNewFile() && !file.exists())
                throw new RuntimeException("Could not create save file for [" + path + "]");
        }
        try (FileWriter writer = new FileWriter(file)) {
            attachedPipeline.getGson().toJson(dataToSave, writer);
        }
    }

    private JsonElement loadFromFile(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID) throws IOException {
        Path path = getFilePath(dataClass, objectUUID);
        File file = new File(path.toUri());
        if (!file.exists())
            throw new RuntimeException("Savefile does not exist for " + dataClass.getSimpleName() + ":" + objectUUID);
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(path.toFile()))) {
            return JsonParser.parseReader(bufferedReader);
        }
    }

    private Path getParentFolder(@NotNull Class<? extends IPipelineData> dataClass) {
        return Path.of(this.path + "//" + getStoragePath(dataClass, getSuffix(dataClass), "//"));
    }

    private Path getFilePath(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID) {
        return Path.of(getParentFolder(dataClass) + "//" + objectUUID + ".json");
    }

    @Override
    public void shutdown() {

    }

    @Override
    public void connect() {
        NetworkLogger.info("JsonFileStorage loaded");
    }

    @Override
    public void disconnect() {

    }
}
