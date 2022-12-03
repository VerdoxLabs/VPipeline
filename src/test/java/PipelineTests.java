import de.verdox.vpipeline.api.VNetwork;
import de.verdox.vpipeline.api.messaging.MessagingService;
import de.verdox.vpipeline.api.pipeline.core.Pipeline;
import de.verdox.vpipeline.api.pipeline.datatypes.SynchronizingService;
import de.verdox.vpipeline.api.pipeline.parts.GlobalCache;
import de.verdox.vpipeline.api.pipeline.parts.GlobalStorage;
import de.verdox.vpipeline.api.util.AnnotationResolver;
import model.TestData;
import model.TestQuery;
import model.TestUpdate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 18:18
 */
public class PipelineTests {

    public static Pipeline pipeline;
    public static Pipeline remotePipeline;
    public static MessagingService messagingService1;
    public static MessagingService messagingService2;
    public static TestData testData;

    @BeforeAll
    public static void setupPipeline() {
        pipeline = VNetwork
                .getConstructionService()
                .createPipeline()
                .withGlobalCache(GlobalCache.createRedisCache(false, new String[]{"redis://localhost:6379"}, ""))
                .withSynchronizingService(SynchronizingService.buildRedisService(false, new String[]{"redis://localhost:6379"}, ""))
                .withGlobalStorage(GlobalStorage.buildMongoDBStorage("127.0.0.1", "vPipelineTest", 27017, "", ""))
                .buildPipeline();

        pipeline.getDataRegistry().registerType(TestData.class);

        remotePipeline = VNetwork
                .getConstructionService()
                .createPipeline()
                .withGlobalCache(GlobalCache.createRedisCache(false, new String[]{"redis://localhost:6379"}, ""))
                .withSynchronizingService(SynchronizingService.buildRedisService(false, new String[]{"redis://localhost:6379"}, ""))
                .withGlobalStorage(GlobalStorage.buildMongoDBStorage("127.0.0.1", "vPipelineTest", 27017, "", ""))
                .buildPipeline();

        remotePipeline.getDataRegistry().registerType(TestData.class);

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
    public void testConcurrency() throws ExecutionException, InterruptedException {
        var uuid = UUID.nameUUIDFromBytes("test".getBytes(StandardCharsets.UTF_8));
/*
        var lock = remotePipeline
                .loadOrCreate(TestData.class, uuid)
                .get();

        var tasksComplete = new CompletableFuture<Boolean>();

        lock
                .performWriteOperation(testData1 -> testData1.testInt = 0, true)
                .thenApply(pipelineLock -> pipelineLock.performWriteOperation(testData1 -> {
                            for (int i = 0; i < 100; i++)
                                testData.testInt += 1;
                        }, true).join()
                );

        lock.performWriteOperation(testData1 -> testData1.testInt = 0, true).thenApply(pipelineLock -> {
            var data = pipeline.loadOrCreate(TestData.class, uuid);

            try {
                data.thenAccept(testDataPipelineLock -> testDataPipelineLock.performWriteOperation(testData -> {
                    for (int i = 0; i < 100; i++)
                        testData.testInt += 1;
                }, true)).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }

            data.thenAccept(testDataPipelineLock -> testDataPipelineLock.performWriteOperation(testData -> {
                for (int i = 0; i < 100; i++)
                    testData.testInt += 1;
            }, true));

            var remoteData = remotePipeline.loadOrCreate(TestData.class, uuid);

            remoteData.thenAccept(testDataPipelineLock -> testDataPipelineLock.performWriteOperation(testData -> {
                for (int i = 0; i < 100; i++)
                    testData.testInt += 1;
            }, true));

            tasksComplete.complete(true);
            return pipelineLock;
        });

        tasksComplete.get();

        var testInt = remotePipeline
                .loadOrCreate(TestData.class, uuid)
                .get()
                .syncGetter(testData1 -> testData1.testInt);
        assertEquals(300, testInt, "Testint is not " + 400);*/
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
            pipeline.delete(testData.getClass(), testData.getObjectUUID());

        pipeline.shutdown();
        remotePipeline.shutdown();

        messagingService1.shutdown();
        messagingService2.shutdown();
    }
}
