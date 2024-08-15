package de.verdox.vpipeline.api.pipeline.parts.cache.local;

public class AccessInvalidException extends Exception {
    public AccessInvalidException() {
    }

    public AccessInvalidException(String message) {
        super(message);
    }

    public AccessInvalidException(String message, Throwable cause) {
        super(message, cause);
    }

    public AccessInvalidException(Throwable cause) {
        super(cause);
    }

    public AccessInvalidException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
