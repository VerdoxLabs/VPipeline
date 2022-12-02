import de.verdox.vpipeline.api.VNetwork;
import de.verdox.vpipeline.api.messaging.MessagingService;
import de.verdox.vpipeline.api.pipeline.core.Pipeline;
import de.verdox.vpipeline.api.pipeline.datatypes.SynchronizingService;
import de.verdox.vpipeline.api.pipeline.parts.GlobalCache;
import de.verdox.vpipeline.api.pipeline.parts.GlobalStorage;
import de.verdox.vpipeline.api.util.AnnotationResolver;
import model.TestData;
import model.TestPing;
import model.TestQuery;
import model.TestUpdate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 18:18
 */
public class PipelineTests {

    public static Pipeline pipeline1;
    public static Pipeline pipeline2;
    public static MessagingService messagingService1;
    public static MessagingService messagingService2;
    public static TestData testData;

    @BeforeAll
    public static void setupPipeline() throws ExecutionException, InterruptedException {
        pipeline1 = VNetwork
                .getConstructionService()
                .createPipeline()
                .withGlobalCache(GlobalCache.createRedisCache(false, new String[]{"redis://localhost:6379"}, ""))
                .withSynchronizingService(SynchronizingService.buildRedisService(false, new String[]{"redis://localhost:6379"}, ""))
                .withGlobalStorage(GlobalStorage.buildMongoDBStorage("127.0.0.1", "vPipelineTest", 27017, "", ""))
                .buildPipeline();

        pipeline1.getDataRegistry().registerType(TestData.class);

        pipeline2 = VNetwork
                .getConstructionService()
                .createPipeline()
                .withGlobalCache(GlobalCache.createRedisCache(false, new String[]{"redis://localhost:6379"}, ""))
                .withSynchronizingService(SynchronizingService.buildRedisService(false, new String[]{"redis://localhost:6379"}, ""))
                .withGlobalStorage(GlobalStorage.buildMongoDBStorage("127.0.0.1", "vPipelineTest", 27017, "", ""))
                .buildPipeline();

        pipeline2.getDataRegistry().registerType(TestData.class);

        messagingService1 = VNetwork
                .getConstructionService()
                .createMessagingService()
                .withIdentifier("server1")
                .useRedisTransmitter(false, new String[]{"redis://localhost:6379"}, "")
                .buildMessagingService();

        messagingService2 = VNetwork
                .getConstructionService()
                .createMessagingService()
                .withIdentifier("server2")
                .useRedisTransmitter(false, new String[]{"redis://localhost:6379"}, "")
                .buildMessagingService();

        messagingService1.getMessageFactory().registerInstructionType(0, TestUpdate.class);
        messagingService2.getMessageFactory().registerInstructionType(0, TestUpdate.class);

        messagingService1.getMessageFactory().registerInstructionType(1, TestQuery.class);
        messagingService2.getMessageFactory().registerInstructionType(1, TestQuery.class);

        //testData = pipeline1.loadOrCreate(TestData.class, UUID.randomUUID()).get();
    }

    @Test
    public void testAllowance() {
        assertTrue(AnnotationResolver.getDataProperties(TestData.class).dataContext().isCacheAllowed());
        assertTrue(AnnotationResolver.getDataProperties(TestData.class).dataContext().isStorageAllowed());
    }

    @Test
    public void testChangeDataRuntime1() throws ExecutionException, InterruptedException {
/*        testData = pipeline1.loadOrCreate(TestData.class, UUID.randomUUID()).get();
        testData.testInt = 1;
        var dataExists = pipeline2.exist(TestData.class, testData.getObjectUUID()).get();
        assertTrue(dataExists);
        TestData dataFromOtherPipeline = pipeline2.load(TestData.class, testData.getObjectUUID()).get();
        testData.save(true);

        assertEquals(1, dataFromOtherPipeline.testInt);*/
    }

    @Test
    public void testUpdate1() {
        var update = TestUpdate.createInstruction(TestUpdate.class).withData("example");
        messagingService1.sendInstruction(update);
        var updateSuccessful = update.getFuture().getOrDefault(2, TimeUnit.SECONDS, false);
        assertTrue(updateSuccessful);
    }

    @AfterAll
    public static void cleanUp() {
        if (testData != null)
            pipeline1.delete(testData.getClass(), testData.getObjectUUID());

        pipeline1.shutdown();
        pipeline2.shutdown();

        messagingService1.shutdown();
        messagingService2.shutdown();
    }
}
