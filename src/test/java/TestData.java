import de.verdox.vpipeline.api.pipeline.core.IPipeline;
import de.verdox.vpipeline.api.pipeline.datatypes.PipelineData;
import de.verdox.vpipeline.api.pipeline.enums.DataContext;
import de.verdox.vpipeline.api.pipeline.annotations.DataStorageIdentifier;
import de.verdox.vpipeline.api.pipeline.annotations.PipelineDataProperties;
import de.verdox.vpipeline.api.pipeline.enums.PreloadStrategy;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 22:20
 */

@DataStorageIdentifier(identifier = "TestData")
@PipelineDataProperties(dataContext = DataContext.GLOBAL, preloadStrategy = PreloadStrategy.LOAD_ON_NEED)
public class TestData extends PipelineData {

    public String testString;
    public int testInt;
    public List<Double> testList;
    public Set<String> testSet;
    public Map<String,Boolean> testMap;

    public TestData(@NotNull IPipeline pipeline, @NotNull UUID objectUUID) {
        super(pipeline, objectUUID);
    }
}
