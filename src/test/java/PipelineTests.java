import de.verdox.vpipeline.api.pipeline.core.IPipeline;
import de.verdox.vpipeline.api.util.AnnotationResolver;
import de.verdox.vpipeline.api.VPipeline;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 18:18
 */
public class PipelineTests {

    public static IPipeline pipeline1;
    public static IPipeline pipeline2;
    public static TestData testData;

    @BeforeAll
    public static void setupPipeline() {
        pipeline1 = VPipeline
                .createPipeline()
                .useRedisCache(false, new String[]{"redis://localhost:6379"}, "")
                .useRedisSynchronizationService(false, new String[]{"redis://localhost:6379"}, "")
                .useMongoStorage("127.0.0.1", "vPipelineTest", 27017, "", "")
                .buildPipeline();

        pipeline1.getDataRegistry().registerType(TestData.class);

        pipeline2 = VPipeline
                .createPipeline()
                .useRedisCache(false, new String[]{"redis://localhost:6379"}, "")
                .useRedisSynchronizationService(false, new String[]{"redis://localhost:6379"}, "")
                .useMongoStorage("127.0.0.1", "vPipelineTest", 27017, "", "")
                .buildPipeline();

        pipeline2.getDataRegistry().registerType(TestData.class);
    }

    @Test
    public void testAllowance() {
        assertTrue(AnnotationResolver.getDataProperties(TestData.class).dataContext().isCacheAllowed());
        assertTrue(AnnotationResolver.getDataProperties(TestData.class).dataContext().isStorageAllowed());
    }

    @Test
    public void changeData1() {
        testData = pipeline1.load(TestData.class, UUID.randomUUID(), IPipeline.LoadingStrategy.LOAD_PIPELINE, true);
        testData.testInt = 1;
        testData.save(true);

        TestData dataFromOtherPipeline = pipeline2.load(TestData.class, testData.getObjectUUID(), IPipeline.LoadingStrategy.LOAD_PIPELINE, true);
        assertEquals(1, dataFromOtherPipeline.testInt);
    }

    @AfterAll
    public static void cleanUp() {
        pipeline1.delete(testData.getClass(), testData.getObjectUUID(), true, IPipeline.QueryStrategy.ALL);
    }
}
