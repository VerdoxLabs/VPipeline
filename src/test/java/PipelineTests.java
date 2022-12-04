import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.VNetwork;
import de.verdox.vpipeline.api.messaging.MessagingService;
import de.verdox.vpipeline.api.pipeline.core.Pipeline;
import de.verdox.vpipeline.api.pipeline.datatypes.SynchronizingService;
import de.verdox.vpipeline.api.pipeline.parts.GlobalCache;
import de.verdox.vpipeline.api.pipeline.parts.GlobalStorage;
import de.verdox.vpipeline.api.util.AnnotationResolver;
import model.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

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

    public static final UUID uuid1 = UUID.randomUUID();

    @BeforeEach
    public void testAnnounce() {
        NetworkLogger.info("<> ================= <>");
    }

    @BeforeAll
    public static void setupPipeline() {
        pipeline = VNetwork
                .getConstructionService()
                .createPipeline()
                .withGlobalCache(GlobalCache.createRedisCache(false, new String[]{"redis://localhost:6379"}, ""))
                .withSynchronizingService(SynchronizingService.buildRedisService(false, new String[]{"redis://localhost:6379"}, ""))
                .withGlobalStorage(GlobalStorage.buildMongoDBStorage("127.0.0.1", "vPipelineTest", 27017, "", ""))
                .buildPipeline();


        remotePipeline = VNetwork
                .getConstructionService()
                .createPipeline()
                .withGlobalCache(GlobalCache.createRedisCache(false, new String[]{"redis://localhost:6379"}, ""))
                .withSynchronizingService(SynchronizingService.buildRedisService(false, new String[]{"redis://localhost:6379"}, ""))
                .withGlobalStorage(GlobalStorage.buildMongoDBStorage("127.0.0.1", "vPipelineTest", 27017, "", ""))
                .buildPipeline();


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

        pipeline.getDataRegistry().registerType(TestData.class);
        pipeline.getDataRegistry().registerType(OnlyLocalData.class);
        pipeline.getDataRegistry().registerType(OnlyCacheData.class);
        pipeline.getDataRegistry().registerType(OnlyStorageData.class);

        remotePipeline.getDataRegistry().registerType(TestData.class);
        remotePipeline.getDataRegistry().registerType(OnlyLocalData.class);
        remotePipeline.getDataRegistry().registerType(OnlyCacheData.class);
        remotePipeline.getDataRegistry().registerType(OnlyStorageData.class);

        messagingService1.getMessageFactory().registerInstructionType(0, TestUpdate.class);
        messagingService2.getMessageFactory().registerInstructionType(0, TestUpdate.class);

        messagingService1.getMessageFactory().registerInstructionType(1, TestQuery.class);
        messagingService2.getMessageFactory().registerInstructionType(1, TestQuery.class);

        //testData = pipeline1.loadOrCreate(TestData.class, UUID.randomUUID()).get();
    }

    @Test
    public void testChangeData1() {
        pipeline.loadOrCreate(TestData.class, uuid1)
                .thenApply(pipelineLock -> pipelineLock.performWriteOperation(testData -> testData.testInt = 0))
                .thenApply(pipelineLock -> pipelineLock.performWriteOperation(testData -> testData.testInt += 1))
                .thenApply(pipelineLock -> pipelineLock.performWriteOperation(testData -> testData.testString = "hallo"))
                .join();

        var testInt = pipeline.load(TestData.class, uuid1)
                              .thenApply(pipelineLock -> pipelineLock.getter(testData1 -> testData1.testInt)).join();
        pipeline.delete(TestData.class, uuid1).join();
        assertEquals(1, testInt, "Testint should be 1");
    }

    @Test
    public void testChangeData2() throws InterruptedException {
        pipeline
                .loadOrCreate(TestData.class, uuid1)
                .thenApply(pipelineLock -> pipelineLock.performWriteOperation(testData -> testData.testInt += 1))
                .join();

        var t1 = new Thread(() -> {
            remotePipeline
                    .loadOrCreate(TestData.class, uuid1)
                    .thenApply(pipelineLock -> pipelineLock.performWriteOperation(testData -> testData.testInt += 1))
                    .join();
        });

        var t2 = new Thread(() -> {
            remotePipeline
                    .loadOrCreate(TestData.class, uuid1)
                    .thenApply(pipelineLock -> pipelineLock.performWriteOperation(testData -> testData.testInt += 1))
                    .join();
        });
        t1.start();
        t2.start();

        t1.join();
        t2.join();

        var testInt = pipeline.load(TestData.class, uuid1)
                              .thenApply(pipelineLock -> pipelineLock.getter(testData1 -> testData1.testInt)).join();

        assertEquals(3, testInt, "should be 3");
    }

    @Test
    public void testChangeData3() throws InterruptedException {
        pipeline
                .loadOrCreate(TestData.class, uuid1)
                .thenApply(pipelineLock -> pipelineLock.performWriteOperation(testData -> testData.testInt = 0))
                .join();

        var t1 = new Thread(() -> {
            remotePipeline.loadOrCreate(TestData.class, uuid1).thenApply(pipelineLock -> {
                for (int i = 0; i < 100; i++)
                    pipelineLock.performWriteOperation(testData -> testData.testInt += 1);
                return pipelineLock;
            }).join();
        });

        var t2 = new Thread(() -> {
            remotePipeline.loadOrCreate(TestData.class, uuid1).thenApply(pipelineLock -> {
                for (int i = 0; i < 100; i++)
                    pipelineLock.performWriteOperation(testData -> testData.testInt += 1);
                return pipelineLock;
            }).join();
        });
        t1.start();
        t2.start();

        t1.join();
        t2.join();

        var testInt = pipeline.load(TestData.class, uuid1)
                              .thenApply(pipelineLock -> pipelineLock.getter(testData1 -> testData1.testInt)).join();

        assertEquals(200, testInt, "should be 200");
    }

    @Test
    public void testChangeAndRemove1() throws InterruptedException {

        pipeline.loadOrCreate(TestData.class, uuid1).join();

        var t1 = new Thread(() -> {
            remotePipeline.loadOrCreate(TestData.class, uuid1).join();
            remotePipeline.delete(TestData.class, uuid1).join();
        });
        t1.start();
        t1.join();

        var existsInRemotePipeline = remotePipeline.exist(TestData.class, uuid1).join();
        var exists = pipeline.exist(TestData.class, uuid1).join();
        var existsLocally = pipeline.getLocalCache().dataExist(TestData.class, uuid1);
        var existsGlobalCache = pipeline.getGlobalCache().dataExist(TestData.class, uuid1);
        var existsGlobalStorage = pipeline.getGlobalStorage().dataExist(TestData.class, uuid1);

        assertFalse(existsInRemotePipeline, "Data should not exist remote in pipeline anymore");
        assertFalse(existsGlobalStorage, "Data should not exist in global storage anymore");
        assertFalse(existsGlobalCache, "Data should not exist in global cache anymore");
        assertFalse(existsLocally, "Data should not exist in local cache anymore");
        assertFalse(exists, "Data should not exist at all");
    }

    @Test
    public void testChangeAndRemove2() {

        pipeline.loadOrCreate(TestData.class, uuid1).join();

        var t1 = new Thread(() -> remotePipeline.delete(TestData.class, uuid1));
        t1.start();
        pipeline
                .load(TestData.class, uuid1)
                .thenApply(pipelineLock -> pipelineLock.performWriteOperation(testData1 -> testData1.testString = "a"))
                .join();

        var existsInRemotePipeline = remotePipeline.exist(TestData.class, uuid1).join();
        var exists = pipeline.exist(TestData.class, uuid1).join();
        var existsLocally = pipeline.getLocalCache().dataExist(TestData.class, uuid1);
        var existsGlobalCache = pipeline.getGlobalCache().dataExist(TestData.class, uuid1);
        var existsGlobalStorage = pipeline.getGlobalStorage().dataExist(TestData.class, uuid1);

        assertFalse(existsInRemotePipeline, "Data should not exist remote in pipeline anymore");
        assertFalse(existsGlobalStorage, "Data should not exist in global storage anymore");
        assertFalse(existsGlobalCache, "Data should not exist in global cache anymore");
        assertFalse(existsLocally, "Data should not exist in local cache anymore");
        assertFalse(exists, "Data should not exist at all");
    }

    @Test
    public void testRemoveAndCreate1() {
        pipeline
                .loadOrCreate(TestData.class, uuid1)
                .thenApply(pipelineLock -> pipelineLock.performWriteOperation(testData1 -> testData1.testString = "Peter"))
                .join();
        pipeline.delete(TestData.class, uuid1).join();
        var string = pipeline
                .loadOrCreate(TestData.class, uuid1)
                .thenApply(pipelineLock -> pipelineLock.getter(testData1 -> testData1.testString))
                .join();
        assertNull(string);
    }

    @Test
    public void testOnlyLocal() {
        pipeline
                .loadOrCreate(OnlyLocalData.class, uuid1)
                .thenApply(pipelineLock -> pipelineLock.performWriteOperation(onlyLocalData -> {
                }));

        var existsInCache = pipeline.getGlobalCache().dataExist(OnlyLocalData.class, uuid1);
        var existsInStorage = pipeline.getGlobalStorage().dataExist(OnlyLocalData.class, uuid1);
        assertFalse(existsInCache);
        assertFalse(existsInStorage);
    }

    @Test
    public void testOnlyCache() {
        pipeline.loadOrCreate(OnlyCacheData.class, uuid1)
                .join();

        var existsInCache = pipeline.getGlobalCache().dataExist(OnlyCacheData.class, uuid1);
        var existsInStorage = pipeline.getGlobalStorage().dataExist(OnlyCacheData.class, uuid1);

        assertTrue(existsInCache);
        assertFalse(existsInStorage);
    }

    @AfterAll
    public static void cleanUp() {
        if (testData != null)
            pipeline.delete(testData.getClass(), testData.getObjectUUID());

        pipeline.delete(TestData.class, uuid1);

        pipeline.shutdown();
        remotePipeline.shutdown();

        messagingService1.shutdown();
        messagingService2.shutdown();
    }
}
