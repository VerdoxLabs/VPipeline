package de.verdox.vpipeline.api.pipeline.enums;

public enum DataContext {
    GLOBAL,
    CACHE_ONLY,
    STORAGE_ONLY,
    LOCAL;

    public boolean isCacheAllowed() {
        return this.equals(CACHE_ONLY) || this.equals(GLOBAL);
    }

    public boolean isStorageAllowed() {
        return this.equals(STORAGE_ONLY) || this.equals(GLOBAL);
    }
}
