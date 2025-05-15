package de.verdox.vpipeline.api.pipeline.parts.storage;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mongodb.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import de.verdox.vpipeline.api.Connection;
import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.modules.AttachedPipeline;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import de.verdox.vpipeline.api.pipeline.parts.GlobalStorage;
import de.verdox.vserializer.SerializableField;
import de.verdox.vserializer.bson.BsonSerializerContext;
import de.verdox.vserializer.generic.SerializationElement;
import de.verdox.vserializer.generic.Serializer;
import de.verdox.vserializer.generic.SerializerBuilder;
import de.verdox.vserializer.json.JsonSerializationElement;
import de.verdox.vserializer.json.JsonSerializerContext;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class MongoDBStorage implements GlobalStorage, Connection {
    public static final Serializer<MongoDBStorage> SERIALIZER = SerializerBuilder.create("mongo_db_storage", MongoDBStorage.class)
            .constructor(
                    new SerializableField<>("host", Serializer.Primitive.STRING, mongoDBStorage -> mongoDBStorage.host),
                    new SerializableField<>("database", Serializer.Primitive.STRING, mongoDBStorage -> mongoDBStorage.database),
                    new SerializableField<>("port", Serializer.Primitive.INTEGER, mongoDBStorage -> mongoDBStorage.port),
                    new SerializableField<>("user", Serializer.Primitive.STRING, mongoDBStorage -> mongoDBStorage.user),
                    new SerializableField<>("password", Serializer.Primitive.STRING, mongoDBStorage -> mongoDBStorage.password),
                    new SerializableField<>("url", Serializer.Primitive.STRING, mongoDBStorage -> mongoDBStorage.url),
                    MongoDBStorage::new
            )
            .build();
    public static final JsonSerializerContext JSON_SERIALIZER_CONTEXT = new JsonSerializerContext();

    private MongoClient mongoClient;
    private MongoDatabase mongoDatabase;
    private final String host;
    private final String database;
    private final int port;
    private final String user;
    private final String password;
    private final AttachedPipeline attachedPipeline;
    private final String url;
    private final BsonSerializerContext bsonSerializerContext = new BsonSerializerContext();
    //private final CodecRegistry codecRegistry;

    public MongoDBStorage(String host, String database, int port, String user, String password, String url) {
        this.url = url;
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
    }

    public MongoDBStorage(String host, String database, int port) {
        this(host, database, port, "", "", "");
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

        SerializationElement bsonElement = bsonSerializerContext.toElement(attachedPipeline.getGson().toJsonTree(mongoDBData));
        return ((JsonSerializationElement) JSON_SERIALIZER_CONTEXT.convert(bsonElement, true)).getJsonElement();
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
            newData.putAll(Document.parse(attachedPipeline.getGson().toJson(bsonSerializerContext.toElement(dataToSave).getJsonElement())));
            collection.insertOne(newData);
        } else {
            Document newData = Document.parse(attachedPipeline.getGson().toJson(bsonSerializerContext.toElement(dataToSave).getJsonElement()));
            Document updateFunc = new Document("$set", newData);
            collection.updateOne(filter, updateFunc);
        }
    }

    @Override
    public boolean remove(@NotNull Class<? extends IPipelineData> dataClass, @NotNull UUID objectUUID) {
        Objects.requireNonNull(dataClass, "dataClass can't be null!");
        Objects.requireNonNull(objectUUID, "objectUUID can't be null!");

        Document filter = new Document("objectUUID", objectUUID.toString());

        if (!dataExist(dataClass, objectUUID))
            return true;

        MongoCollection<Document> collection = getMongoStorage(dataClass, getSuffix(dataClass));

        var result = collection.deleteOne(filter).getDeletedCount() >= 1;
        if (!result)
            NetworkLogger.getLogger().warning("Could not delete data from MongoDB");
        return result;
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

        var clientOptions = new MongoClientOptions.Builder();

        if (this.url != null && !this.url.isEmpty()) {
            NetworkLogger.info("Connecting to MongoDB GlobalStorage with url:" + url);
            this.mongoClient = new MongoClient(new MongoClientURI(url));
        } else if (user.isEmpty() && password.isEmpty()) {
            NetworkLogger.info("Connecting to MongoDB GlobalStorage with host " + host + ":" + port + " using no credentials");
            this.mongoClient = new MongoClient(new ServerAddress(host, port), clientOptions.build());
        } else {
            NetworkLogger.info("Connecting to MongoDB GlobalStorage with host " + host + ":" + port + " using credentials");
            this.mongoClient = new MongoClient(new ServerAddress(host, port), MongoCredential.createScramSha256Credential(user, database, password.toCharArray()), clientOptions.build());
        }
        NetworkLogger.info("Searching for database " + database + " in MongoDB storage...");
        this.mongoDatabase = mongoClient.getDatabase(database);
        NetworkLogger.info("MongoDB GlobalStorage connected");
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
