package model.data;

import de.verdox.vpipeline.api.pipeline.annotations.DataStorageIdentifier;
import de.verdox.vpipeline.api.pipeline.annotations.PipelineDataProperties;
import de.verdox.vpipeline.api.pipeline.core.Pipeline;
import de.verdox.vpipeline.api.pipeline.datatypes.PipelineData;
import de.verdox.vpipeline.api.pipeline.enums.DataContext;
import de.verdox.vpipeline.api.pipeline.enums.PreloadStrategy;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@DataStorageIdentifier(identifier = "model.data.LoadBeforeTest", classifier = "test")
@PipelineDataProperties(dataContext = DataContext.GLOBAL, preloadStrategy = PreloadStrategy.LOAD_BEFORE)
public class LoadBeforeTest extends PipelineData {

    public int testInt;

    public LoadBeforeTest(@NotNull Pipeline pipeline, @NotNull UUID objectUUID) {
        super(pipeline, objectUUID);
    }
}
