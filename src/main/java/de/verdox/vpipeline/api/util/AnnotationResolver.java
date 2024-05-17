package de.verdox.vpipeline.api.util;

import de.verdox.vpipeline.api.pipeline.annotations.DataStorageIdentifier;
import de.verdox.vpipeline.api.pipeline.annotations.PipelineDataProperties;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import org.jetbrains.annotations.NotNull;

public class AnnotationResolver {
    /**
     * Searches for DataStorageIdentifier and returns the identifier specified in the Annotation
     * This value is used for VCore Storage implementations to identify data.
     *
     * @param customClass Class to search Annotation for
     * @return The Data Identifier of the class
     */

    @NotNull
    public static String getDataStorageIdentifier(Class<?> customClass) {
        DataStorageIdentifier dataStorageIdentifier = customClass.getAnnotation(DataStorageIdentifier.class);
        if (dataStorageIdentifier == null)
            throw new NullPointerException("DataStorageIdentifier not set for class: " + customClass);
        return dataStorageIdentifier.identifier();
    }

    @NotNull
    public static String getDataStorageClassifier(Class<?> customClass) {
        DataStorageIdentifier dataStorageIdentifier = customClass.getAnnotation(DataStorageIdentifier.class);
        if (dataStorageIdentifier == null)
            throw new NullPointerException("DataStorageIdentifier not set for class: " + customClass);
        return dataStorageIdentifier.classifier();
    }


    /**
     * Searches for VCoreDataProperties Annotation of a class and returns it.
     *
     * @param classType The VCoreDataTypeClass to check
     * @return The Found VCoreDataProperties Instance if exists
     * @throws RuntimeException when no Annotation was found
     */

    @NotNull
    public static PipelineDataProperties getDataProperties(Class<? extends IPipelineData> classType) {
        PipelineDataProperties pipelineDataProperties = classType.getAnnotation(PipelineDataProperties.class);
        if (pipelineDataProperties == null)
            throw new RuntimeException(classType.getName() + " does not have PipelineDataProperties Annotation set");
        return pipelineDataProperties;
    }
}
