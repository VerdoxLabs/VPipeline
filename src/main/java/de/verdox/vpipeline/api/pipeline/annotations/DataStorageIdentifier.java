package de.verdox.vpipeline.api.pipeline.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DataStorageIdentifier {
    /**
     * The classifier a {@link de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData} in the pipeline structure.
     * A classifier is like a group prefix.
     *
     * @return the classifier
     */
    String classifier() default "";

    /**
     * The unique identifier of a {@link de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData} in the pipeline structure.
     *
     * @return the identifier
     */
    String identifier();
}