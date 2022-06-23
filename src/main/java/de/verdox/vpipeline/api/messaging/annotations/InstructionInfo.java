package de.verdox.vpipeline.api.messaging.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 19.06.2022 12:37
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface InstructionInfo {
    boolean awaitsResponse();
}
