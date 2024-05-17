package de.verdox.vpipeline.api.pipeline.core;

public enum LoadingStrategy {
    // Data will be loaded from Local Cache
    LOAD_LOCAL,
    // Data will be loaded from local Cache if not cached it will be loaded into local cache async for the next possible try
    LOAD_LOCAL_ELSE_LOAD,
    // Loads data from PipeLine
    LOAD_PIPELINE,
}
