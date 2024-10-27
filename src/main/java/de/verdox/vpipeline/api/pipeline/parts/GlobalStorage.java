package de.verdox.vpipeline.api.pipeline.parts;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.verdox.vserializer.json.JsonSerializer;
import de.verdox.vpipeline.api.Connection;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import de.verdox.vpipeline.api.pipeline.parts.storage.JsonFileStorage;
import de.verdox.vpipeline.api.pipeline.parts.storage.MongoDBStorage;
import de.verdox.vpipeline.api.pipeline.parts.storage.MySQLStorage;
import de.verdox.vpipeline.api.util.AnnotationResolver;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Objects;

public interface GlobalStorage extends DataProvider, Connection {

    JsonSerializer<GlobalStorage> SERIALIZER = JsonSerializer.Selection.create("global_cache", GlobalStorage.class)
            .variant("json", JsonFileStorage.SERIALIZER, new JsonFileStorage(Path.of("storage")))
            .variant("mongo", MongoDBStorage.SERIALIZER, new MongoDBStorage("127.0.0.1", "pipeline", 27017, "root", "password", "https://mongoURL"))
            ;

    default String getSuffix(@NotNull Class<? extends IPipelineData> dataClass) {
        Objects.requireNonNull(dataClass, "dataClass can't be null!");
        return AnnotationResolver.getDataStorageIdentifier(dataClass);
    }

    default String getStoragePath(@NotNull Class<? extends IPipelineData> dataClass, @NotNull String suffix, @NotNull String separator) {
        Objects.requireNonNull(dataClass, "dataClass can't be null!");
        Objects.requireNonNull(suffix, "suffix can't be null!");
        return AnnotationResolver.getDataStorageClassifier(dataClass) + separator + suffix;
    }

    static GlobalStorage buildMongoDBStorage(String host, String database, int port, String user, String password) {
        return new MongoDBStorage(host, database, port, user, password, "");
    }

    static GlobalStorage buildMongoDBStorage(String url) {
        return new MongoDBStorage("", "", 0, "", "", url);
    }

    static GlobalStorage buildJsonStorage(Path path) {
        return new JsonFileStorage(path);
    }

    static GlobalStorage buildSQLStorage(HikariConfig hikariConfig) {
        return new MySQLStorage(hikariConfig);
    }
}