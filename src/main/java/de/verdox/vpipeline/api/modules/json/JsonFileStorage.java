package de.verdox.vpipeline.api.modules.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.modules.AttachedPipeline;
import de.verdox.vpipeline.api.pipeline.core.Pipeline;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import de.verdox.vpipeline.api.pipeline.parts.DataProviderLock;
import de.verdox.vpipeline.api.pipeline.parts.GlobalStorage;
import de.verdox.vpipeline.impl.pipeline.parts.DataProviderLockImpl;
import jodd.io.FileNameUtil;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 17:10
 */
public class JsonFileStorage implements GlobalStorage {
    private final Path path;
    private final AttachedPipeline attachedPipeline;
    private final DataProviderLock dataProviderLock = new DataProviderLockImpl();

    public JsonFileStorage(Path path) {
        this.path = path;
        this.attachedPipeline = new AttachedPipeline(GsonBuilder::create);
        NetworkLogger.info("JsonFileStorage loaded");
    }

    @Override
    public JsonElement loadData(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID) {
        Objects.requireNonNull(dataClass, "dataClass can't be null!");
        Objects.requireNonNull(objectUUID, "objectUUID can't be null!");

        return dataProviderLock.executeOnWriteLock(() -> {
            try {
                return loadFromFile(dataClass, objectUUID);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    @Override
    public boolean dataExist(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID) {
        Objects.requireNonNull(dataClass, "dataClass can't be null!");
        Objects.requireNonNull(objectUUID, "objectUUID can't be null!");
        return dataProviderLock.executeOnReadLock(() -> Files.exists(getFilePath(dataClass, objectUUID)));
    }

    @Override
    public void save(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID, @NotNull JsonElement dataToSave) {
        Objects.requireNonNull(dataClass, "dataClass can't be null!");
        Objects.requireNonNull(objectUUID, "objectUUID can't be null!");
        dataProviderLock.executeOnWriteLock(() -> {
            try {
                saveJsonToFile(dataClass, objectUUID, dataToSave);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    @Override
    public boolean remove(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID) {
        Objects.requireNonNull(dataClass, "dataClass can't be null!");
        Objects.requireNonNull(objectUUID, "objectUUID can't be null!");
        return dataProviderLock.executeOnWriteLock(() -> {
            if (!dataExist(dataClass, objectUUID))
                return false;
            try {
                Files.deleteIfExists(getFilePath(dataClass, objectUUID));
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    @Override
    public Set<UUID> getSavedUUIDs(@NotNull Class<? extends IPipelineData> dataClass) {
        Objects.requireNonNull(dataClass, "dataClass can't be null!");
        return dataProviderLock.executeOnReadLock(() -> {
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
        });
    }

    @Override
    public AttachedPipeline getAttachedPipeline() {
        return attachedPipeline;
    }

    @Override
    public DataProviderLock getDataProviderLock() {
        return dataProviderLock;
    }

    private void saveJsonToFile(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID, @NotNull JsonElement dataToSave) throws IOException {
        if (dataToSave.isJsonNull())
            return;
        Path path = getFilePath(dataClass, objectUUID);

        File file = new File(path.toUri());
        if (!file.exists()) {
            if (!file.getParentFile().mkdirs() || !file.createNewFile())
                throw new RuntimeException("Could not create files for JsonFileStorage [" + path + "]");
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
}
