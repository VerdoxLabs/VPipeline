package de.verdox.vpipeline.api.pipeline.datatypes.customtypes;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.pipeline.core.Pipeline;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import de.verdox.vpipeline.api.pipeline.datatypes.PipelineData;
import de.verdox.vpipeline.api.util.AnnotationResolver;

import java.io.IOException;
import java.util.UUID;

public class DataReference<T extends IPipelineData> {
    private final T referencedObject;

    public DataReference(T referencedObject) {
        this.referencedObject = referencedObject;
    }

    public T getReferencedObject() {
        return referencedObject;
    }

    public static class ReferenceAdapter extends TypeAdapter<DataReference<?>> {
        private final Pipeline pipeline;

        public ReferenceAdapter(Pipeline pipeline) {
            this.pipeline = pipeline;
        }

        @Override
        public void write(JsonWriter jsonWriter, DataReference<?> dataReference) throws IOException {
            var storageID = AnnotationResolver.getDataStorageIdentifier(dataReference.referencedObject.getClass());
            jsonWriter.value(storageID);
            jsonWriter.value(dataReference.getReferencedObject().getObjectUUID().toString());
        }

        @Override
        public DataReference<?> read(JsonReader jsonReader) throws IOException {
            var dataStorageID = jsonReader.nextString();
            var uuidString = jsonReader.nextString();
            var type = pipeline.getDataRegistry().getTypeByStorageId(dataStorageID);
            if (type == null) {
                NetworkLogger.getLogger().warning("No type found for storageID " + dataStorageID);
                return null;
            }
            var data = PipelineData.instantiateData(pipeline, type, UUID.fromString(uuidString));
            return new DataReference<>(data);
        }
    }
}
