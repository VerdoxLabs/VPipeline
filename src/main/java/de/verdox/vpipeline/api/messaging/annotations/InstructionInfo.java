package de.verdox.vpipeline.api.messaging.annotations;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;



/**
 * Used to specify whether an instruction should wait for a response.
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface InstructionInfo {
    boolean awaitsResponse();
}
