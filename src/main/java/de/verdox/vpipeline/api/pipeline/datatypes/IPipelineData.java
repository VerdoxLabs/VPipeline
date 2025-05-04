package de.verdox.vpipeline.api.pipeline.datatypes;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import de.verdox.vserializer.generic.Serializer;
import de.verdox.vpipeline.api.modules.AttachedPipeline;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface IPipelineData {

    /**
     * Gets the uuid of the pipeline data
     * @return the object uuid
     */
    UUID getObjectUUID();

    /**
     * Executed after a DataManipulator synced the object
     *
     * @param dataBeforeSync The data the object had before syncing
     */
    default void onSync(String dataBeforeSync) {

    }

    /**
     * Executed after instantiation of the Object
     * Executed before Object is put into LocalCache
     */
    default void onCreate() {
    }

    /**
     * Executed before the object is deleted from local cache.
     */
    default void onDelete() {
    }

    /**
     * Executed directly after Data was loaded from Pipeline. Not if it was found in LocalCache
     */
    default void onLoad() {

    }

    /**
     * Executed before onLoad and before onCreate everytime the data is being loaded into local cache.
     * You can use this function to load dependent data from pipeline that is directly associated with this data
     */
    default void loadDependentData() {

    }

    /**
     * Executed before Data is cleared from LocalCache
     */
    default void onCleanUp() {
    }

    /**
     * TODO: When is this called?
     */
    default void cleanUp() {
        getSynchronizer().cleanUp();
        onCleanUp();
    }

    /**
     * Used to serialize this data object to json
     * @return The serialized data in json format
     */
    JsonElement serialize();

    /**
     * Used to deserialize data from a json object
     * @param jsonObject The data to deserialize the object from
     */
    void deserialize(JsonElement jsonObject);

    /**
     * Used to deserialize data from a json object
     * @param jsonString The data to deserialize the object from
     */
    default void deserialize(String jsonString){
        deserialize(JsonParser.parseString(jsonString));
    }

    @NotNull
    DataSynchronizer getSynchronizer();
    void updateLastUsage();
    void save(boolean saveToStorage);
    AttachedPipeline getAttachedPipeline();

    @Nullable
    default Serializer<? extends IPipelineData> getCustomSerializer(){
        return null;
    }
}
