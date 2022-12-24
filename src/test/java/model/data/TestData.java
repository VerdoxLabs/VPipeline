package model.data;

import de.verdox.vpipeline.api.pipeline.annotations.DataStorageIdentifier;
import de.verdox.vpipeline.api.pipeline.annotations.PipelineDataProperties;
import de.verdox.vpipeline.api.pipeline.core.Pipeline;
import de.verdox.vpipeline.api.pipeline.datatypes.PipelineData;
import de.verdox.vpipeline.api.pipeline.datatypes.customtypes.DataReference;
import de.verdox.vpipeline.api.pipeline.enums.DataContext;
import de.verdox.vpipeline.api.pipeline.enums.PreloadStrategy;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 22:20
 */

@DataStorageIdentifier(identifier = "model.data.TestData",classifier = "test")
@PipelineDataProperties(dataContext = DataContext.GLOBAL, preloadStrategy = PreloadStrategy.LOAD_ON_NEED)
public class TestData extends PipelineData {

    public String testString;
    public int testInt;
    public List<Double> testList;
    public Set<String> testSet;
    public Map<String, Boolean> testMap;

    public Set<DataReference<TestData>> referenceSet = new HashSet<>();
    public Map<UUID, DataReference<TestData>> referenceMap = new HashMap<>();

    public TestData(@NotNull Pipeline pipeline, @NotNull UUID objectUUID) {
        super(pipeline, objectUUID);
    }
}
