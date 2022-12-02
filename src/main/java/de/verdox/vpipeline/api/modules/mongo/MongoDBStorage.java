package de.verdox.vpipeline.api.modules.mongo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import de.verdox.vpipeline.api.modules.AttachedPipeline;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import de.verdox.vpipeline.api.pipeline.parts.GlobalStorage;
import de.verdox.vpipeline.api.pipeline.parts.RemoteStorage;
import org.bson.Document;
import org.checkerframework.checker.units.qual.A;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 17:16
 */
public class MongoDBStorage implements GlobalStorage, RemoteStorage {
    private MongoClient mongoClient;
    private MongoDatabase mongoDatabase;
    private final String host;
    private final String database;
    private final int port;
    private final String user;
    private final String password;
    private final AttachedPipeline attachedPipeline;
    //private final CodecRegistry codecRegistry;

    public MongoDBStorage(String host, String database, int port, String user, String password) {
        Objects.requireNonNull(host, "host can't be null!");
        Objects.requireNonNull(database, "database can't be null!");
        Objects.requireNonNull(user, "user can't be null!");
        Objects.requireNonNull(password, "password can't be null!");
        this.host = host;
        this.database = database;
        this.port = port;
        this.user = user;
        this.password = password;
        //this.codecRegistry = fromRegistries(MongoClient.getDefaultCodecRegistry(),CodecRegistries.fromProviders(new UuidCodecProvider(UuidRepresentation.JAVA_LEGACY)));
        this.attachedPipeline = new AttachedPipeline(GsonBuilder::create);
        connect();
    }

    public MongoDBStorage(String host, String database, int port) {
        this(host, database, port, "", "");
    }


    @Override
    public JsonElement loadData(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID) {
        Objects.requireNonNull(dataClass, "dataClass can't be null!");
        Objects.requireNonNull(objectUUID, "objectUUID can't be null!");
        Document filter = new Document("objectUUID", objectUUID.toString());

        Document mongoDBData = getMongoStorage(dataClass, getSuffix(dataClass)).find(filter).first();

        if (mongoDBData == null)
            mongoDBData = filter;

        mongoDBData.remove("_id");
        return JsonParser.parseString(attachedPipeline.getGson().toJson(mongoDBData));
    }

    @Override
    public boolean dataExist(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID) {
        Objects.requireNonNull(dataClass, "dataClass can't be null!");
        Objects.requireNonNull(objectUUID, "objectUUID can't be null!");
        Document document = getMongoStorage(dataClass, getSuffix(dataClass))
                .find(new Document("objectUUID", objectUUID.toString()))
                .first();
        return document != null;
    }

    @Override
    public void save(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID, @NotNull JsonElement dataToSave) {
        Objects.requireNonNull(dataClass, "dataClass can't be null!");
        Objects.requireNonNull(objectUUID, "objectUUID can't be null!");
        Objects.requireNonNull(dataToSave, "dataToSave can't be null!");
        Document filter = new Document("objectUUID", objectUUID.toString());

        MongoCollection<Document> collection = getMongoStorage(dataClass, getSuffix(dataClass));

        if (collection.find(filter).first() == null) {
            Document newData = new Document("objectUUID", objectUUID.toString());
            newData.putAll(Document.parse(attachedPipeline.getGson().toJson(dataToSave)));
            collection.insertOne(newData);
        } else {
            Document newData = Document.parse(attachedPipeline.getGson().toJson(dataToSave));
            Document updateFunc = new Document("$set", newData);
            collection.updateOne(filter, updateFunc);
        }
    }

    @Override
    public boolean remove(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID) {
        Objects.requireNonNull(dataClass, "dataClass can't be null!");
        Objects.requireNonNull(objectUUID, "objectUUID can't be null!");
        Document filter = new Document("objectUUID", objectUUID.toString());

        MongoCollection<Document> collection = getMongoStorage(dataClass, getSuffix(dataClass));
        return collection.deleteOne(filter).getDeletedCount() >= 1;
    }

    @Override
    public Set<UUID> getSavedUUIDs(@NotNull Class<? extends IPipelineData> dataClass) {
        Objects.requireNonNull(dataClass, "dataClass can't be null!");
        MongoCollection<Document> collection = getMongoStorage(dataClass, getSuffix(dataClass));
        Set<UUID> uuids = new HashSet<>();
        for (Document document : collection.find()) {
            if (!document.containsKey("objectUUID"))
                continue;
            uuids.add(UUID.fromString((String) document.get("objectUUID")));
        }
        return uuids;
    }

    @Override
    public AttachedPipeline getAttachedPipeline() {
        return attachedPipeline;
    }

    private MongoCollection<Document> getMongoStorage(@NotNull Class<? extends IPipelineData> dataClass, @NotNull String suffix) {
        Objects.requireNonNull(dataClass, "dataClass can't be null!");
        Objects.requireNonNull(suffix, "suffix can't be null!");
        String storagePath = getStoragePath(dataClass, suffix, "");
        return getCollection(storagePath);
    }

    private com.mongodb.client.MongoCollection<Document> getCollection(@NotNull String name) {
        Objects.requireNonNull(name, "name can't be null!");
        try {
            return mongoDatabase.getCollection(name);
        }
        // Collection does not exist
        catch (IllegalArgumentException e) {
            mongoDatabase.createCollection(name);
            return mongoDatabase.getCollection(name);
        }
    }

    @Override
    public void connect() {
        if (user.isEmpty() && password.isEmpty())
            this.mongoClient = new MongoClient(host, port);
        else
            this.mongoClient = new MongoClient(new ServerAddress(host, port), MongoCredential.createScramSha256Credential(user, database, password.toCharArray()), new MongoClientOptions.Builder().build());
        this.mongoDatabase = mongoClient.getDatabase(database);
    }

    @Override
    public void disconnect() {
        this.mongoClient.close();
    }

    @Override
    public void shutdown() {
        this.mongoClient.close();
    }
}
