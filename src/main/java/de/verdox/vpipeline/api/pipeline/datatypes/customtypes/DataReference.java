package de.verdox.vpipeline.api.pipeline.datatypes.customtypes;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.pipeline.SynchronizedAccess;
import de.verdox.vpipeline.api.pipeline.core.Pipeline;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import de.verdox.vpipeline.api.util.AnnotationResolver;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public record DataReference<T extends IPipelineData>(SynchronizedAccess<T> referencedAccess) {
    //TODO: Muss geändert werden
    public static <T extends IPipelineData> DataReference<T> of(SynchronizedAccess<T> referencedAccess) {
        return new DataReference<>(referencedAccess);
    }

    public static class ReferenceAdapter extends TypeAdapter<DataReference<?>> {
        private final Pipeline pipeline;

        public ReferenceAdapter(Pipeline pipeline) {
            this.pipeline = pipeline;
        }

        @Override
        public void write(JsonWriter jsonWriter, DataReference<?> dataReference) throws IOException {
            var storageID = AnnotationResolver.getDataStorageIdentifier(dataReference.referencedAccess
                    .getType());

            jsonWriter
                    .beginObject()
                    .name("uuid")
                    .value(dataReference.referencedAccess.getUuid().toString())
                    .name("type")
                    .value(storageID)
                    .endObject();
        }

        @Override
        public DataReference<?> read(JsonReader jsonReader) throws IOException {

            jsonReader.beginObject();
            String fieldName = null;
            UUID uuid = null;
            Class<? extends IPipelineData> type = null;

            while (jsonReader.hasNext() && (uuid == null || type == null)) {
                var token = jsonReader.peek();

                if (token.equals(JsonToken.NAME))
                    fieldName = jsonReader.nextName();
                else if ("uuid".equals(fieldName)) {
                    jsonReader.peek();
                    uuid = UUID.fromString(jsonReader.nextString());
                } else if ("type".equals(fieldName)) {
                    jsonReader.peek();
                    var dataStorageID = jsonReader.nextString();
                    type = pipeline.getDataRegistry().getTypeByStorageId(dataStorageID);
                    if (type == null) {
                        NetworkLogger.getLogger().warning("No type found for storageID " + dataStorageID);
                        return null;
                    }
                }
            }

            if(type == null){
                NetworkLogger.getLogger().warning("Error while reading data reference. Type could not be found");
                return null;
            }
            else if(uuid == null){
                NetworkLogger.getLogger().warning("Error while reading data reference. UUID could not be found");
                return null;
            }
/*            SynchronizedAccess<?> synchronizedAccess;
            try {
                synchronizedAccess = pipeline.load(type, uuid).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }*/
            return new DataReference<>(null);
        }
    }
}
