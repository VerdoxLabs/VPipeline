package de.verdox.vpipeline.api.pipeline.annotations;

import de.verdox.vpipeline.api.pipeline.enums.DataContext;
import de.verdox.vpipeline.api.pipeline.enums.PreloadStrategy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PipelineDataProperties {
    DataContext dataContext() default DataContext.GLOBAL;

    PreloadStrategy preloadStrategy() default PreloadStrategy.LOAD_ON_NEED;

    boolean cleanOnNoUse() default false;

    long time() default 20L;

    TimeUnit timeUnit() default TimeUnit.MINUTES;

    boolean debugMode() default false;
}
