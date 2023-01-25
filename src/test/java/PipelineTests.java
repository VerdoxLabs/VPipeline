import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.NetworkParticipant;
import de.verdox.vpipeline.api.VNetwork;
import de.verdox.vpipeline.api.pipeline.core.Pipeline;
import de.verdox.vpipeline.api.pipeline.datatypes.SynchronizingService;
import de.verdox.vpipeline.api.pipeline.parts.GlobalCache;
import de.verdox.vpipeline.api.pipeline.parts.GlobalStorage;
import io.netty.util.concurrent.DefaultThreadFactory;
import model.data.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @version 1.0
 * @Author: Lukas Jonsson (Verdox)
 * @date 18.06.2022 18:18
 */
public class PipelineTests {

    public static NetworkParticipant networkParticipant1;
    public static NetworkParticipant networkParticipant2;
    public static NetworkParticipant mysqlNetworkParticipant;
    public static NetworkParticipant jsonNetworkParticipant;
    public static Pipeline pipeline;
    public static Pipeline remotePipeline;
    public static TestData testData;
    public static final UUID uuid1 = UUID.nameUUIDFromBytes("test".getBytes(StandardCharsets.UTF_8));
    public static final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2, new DefaultThreadFactory("VPipeline-ThreadPool [PipelineTests]"));

    @BeforeAll
    public static void setupPipeline() {

        NetworkLogger.setLevel(Level.ALL);

        networkParticipant1 = VNetwork
                .getConstructionService()
                .createNetworkParticipant()
                .withExecutorService(scheduledExecutorService)
                .withPipeline(pipelineBuilder -> pipelineBuilder
                        .withGlobalCache(GlobalCache.createRedisCache(false, new String[]{"redis://localhost:6379"}, ""))
                        .withSynchronizingService(SynchronizingService.buildRedisService(false, new String[]{"redis://localhost:6379"}, ""))
                        .withGlobalStorage(GlobalStorage.buildMongoDBStorage("127.0.0.1", "vPipelineTest", 27017, "", "")))
                .withName("server1")
                .build();

        networkParticipant2 = VNetwork
                .getConstructionService()
                .createNetworkParticipant()
                .withExecutorService(scheduledExecutorService)
                .withPipeline(pipelineBuilder -> pipelineBuilder
                        .withGlobalCache(GlobalCache.createRedisCache(false, new String[]{"redis://localhost:6379"}, ""))
                        .withSynchronizingService(SynchronizingService.buildRedisService(false, new String[]{"redis://localhost:6379"}, ""))
                        .withGlobalStorage(GlobalStorage.buildMongoDBStorage("127.0.0.1", "vPipelineTest", 27017, "", "")))
                .withName("server2")
                .build();

        var hikari = new HikariDataSource();
        var host = "localhost";
        var port = 3306;
        var dbName = "vCoreTest";

        var userName = "root";
        var userPassword = "example";

        hikari.setMaximumPoolSize(1);

        NetworkLogger.info("Drivers:");
        DriverManager.getDrivers().asIterator().forEachRemaining(driver -> {
            NetworkLogger.info(driver.getClass().getSimpleName());
        });

        //hikari.setDataSourceClassName("");
        hikari.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + dbName);
        hikari.setUsername(userName);
        hikari.setPassword(userPassword);
        hikari.setIdleTimeout(600000L);
        hikari.setMaxLifetime(1800000L);
        hikari.setLeakDetectionThreshold(60000L);

        mysqlNetworkParticipant = VNetwork
                .getConstructionService()
                .createNetworkParticipant()
                .withExecutorService(scheduledExecutorService)
                .withPipeline(pipelineBuilder -> pipelineBuilder
                        .withGlobalStorage(GlobalStorage.buildSQLStorage(hikari)))
                .withName("server3")
                .build();

        jsonNetworkParticipant = VNetwork.getConstructionService()
                .createNetworkParticipant()
                .withExecutorService(scheduledExecutorService)
                .withPipeline(pipelineBuilder -> pipelineBuilder
                        .withGlobalStorage(GlobalStorage.buildJsonStorage(Path.of("./testJsonStorage"))))
                .withName("server1").build();


        pipeline = networkParticipant1.pipeline();
        remotePipeline = networkParticipant2.pipeline();

/*
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
                .buildMessagingService();*/

        pipeline
                .getDataRegistry()
                .registerType(TestData.class);
        pipeline
                .getDataRegistry()
                .registerType(OnlyLocalData.class);
        pipeline
                .getDataRegistry()
                .registerType(OnlyCacheData.class);
        pipeline
                .getDataRegistry()
                .registerType(OnlyStorageData.class);
        pipeline
                .getDataRegistry()
                .registerType(LoadBeforeTest.class);

        remotePipeline
                .getDataRegistry()
                .registerType(TestData.class);
        remotePipeline
                .getDataRegistry()
                .registerType(OnlyLocalData.class);
        remotePipeline
                .getDataRegistry()
                .registerType(OnlyCacheData.class);
        remotePipeline
                .getDataRegistry()
                .registerType(OnlyStorageData.class);
        remotePipeline
                .getDataRegistry()
                .registerType(LoadBeforeTest.class);

        jsonNetworkParticipant
                .pipeline()
                .getDataRegistry()
                .registerType(TestData.class);

        mysqlNetworkParticipant
                .pipeline()
                .getDataRegistry()
                .registerType(TestData.class);

        pipeline
                .delete(TestData.class, uuid1)
                .join();
        pipeline
                .delete(OnlyLocalData.class, uuid1)
                .join();
        pipeline
                .delete(OnlyCacheData.class, uuid1)
                .join();
        pipeline
                .delete(OnlyStorageData.class, uuid1)
                .join();
        pipeline
                .delete(LoadBeforeTest.class, uuid1)
                .join();

        //testData = pipeline1.loadOrCreate(TestData.class, UUID.randomUUID()).get();
    }

    @Test
    public void testChangeData1() {
        pipeline
                .loadOrCreate(TestData.class, uuid1)
                .thenApply(pipelineLock -> pipelineLock.performWriteOperation(testData -> testData.testInt = 0))
                .thenApply(pipelineLock -> pipelineLock.performWriteOperation(testData -> testData.testInt += 1))
                .thenApply(pipelineLock -> pipelineLock.performWriteOperation(testData -> testData.testString = "hallo"))
                .join();

        var testInt = pipeline
                .load(TestData.class, uuid1)
                .thenApply(pipelineLock -> pipelineLock.getter(testData1 -> testData1.testInt))
                .join();

        remotePipeline.loadAllData(TestData.class);

        pipeline
                .delete(TestData.class, uuid1)
                .join();
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

        var testInt = pipeline
                .load(TestData.class, uuid1)
                .thenApply(pipelineLock -> pipelineLock.getter(testData1 -> testData1.testInt))
                .join();

        assertEquals(3, testInt, "should be 3");
    }

    @Test
    public void testChangeData3() throws InterruptedException {
        pipeline
                .loadOrCreate(TestData.class, uuid1)
                .thenApply(pipelineLock -> pipelineLock.performWriteOperation(testData -> testData.testInt = 0))
                .join();

        var t1 = new Thread(() -> {
            remotePipeline
                    .loadOrCreate(TestData.class, uuid1)
                    .thenApply(pipelineLock -> {
                        for (int i = 0; i < 100; i++)
                            pipelineLock.performWriteOperation(testData -> testData.testInt += 1);
                        return pipelineLock;
                    })
                    .join();
        });

        var t2 = new Thread(() -> {
            remotePipeline
                    .loadOrCreate(TestData.class, uuid1)
                    .thenApply(pipelineLock -> {
                        for (int i = 0; i < 100; i++)
                            pipelineLock.performWriteOperation(testData -> testData.testInt += 1);
                        return pipelineLock;
                    })
                    .join();
        });
        t1.start();
        t2.start();

        t1.join();
        t2.join();

        var testInt = pipeline
                .load(TestData.class, uuid1)
                .thenApply(pipelineLock -> pipelineLock.getter(testData1 -> testData1.testInt))
                .join();

        assertEquals(200, testInt, "should be 200");
    }

    @Test
    public void testChangeAndRemove1() throws InterruptedException {

        pipeline
                .loadOrCreate(TestData.class, uuid1)
                .join();

        var t1 = new Thread(() -> {
            remotePipeline
                    .loadOrCreate(TestData.class, uuid1)
                    .join();
            remotePipeline
                    .delete(TestData.class, uuid1)
                    .join();
        });
        t1.start();
        t1.join();

        var existsInRemotePipeline = remotePipeline
                .exist(TestData.class, uuid1)
                .join();
        var exists = pipeline
                .exist(TestData.class, uuid1)
                .join();
        var existsLocally = pipeline
                .getLocalCache()
                .dataExist(TestData.class, uuid1);
        var existsGlobalCache = pipeline
                .getGlobalCache()
                .dataExist(TestData.class, uuid1);
        var existsGlobalStorage = pipeline
                .getGlobalStorage()
                .dataExist(TestData.class, uuid1);

        assertFalse(existsInRemotePipeline, "Data should not exist remote in pipeline anymore");
        assertFalse(existsGlobalStorage, "Data should not exist in global storage anymore");
        assertFalse(existsGlobalCache, "Data should not exist in global cache anymore");
        assertFalse(existsLocally, "Data should not exist in local cache anymore");
        assertFalse(exists, "Data should not exist at all");
    }

    @Test
    public void testChangeAndRemove2() throws InterruptedException {

        pipeline
                .loadOrCreate(TestData.class, uuid1)
                .join();

        var t1 = new Thread(() -> remotePipeline
                .delete(TestData.class, uuid1)
                .join());
        t1.start();
        t1.join();

        NetworkLogger.info("Testing pipeline:");

        assertFalse(pipeline
                .exist(TestData.class, uuid1)
                .join());
        assertFalse(pipeline
                .getLocalCache()
                .dataExist(TestData.class, uuid1));
        assertFalse(pipeline
                .getGlobalCache()
                .dataExist(TestData.class, uuid1));
        assertFalse(pipeline
                .getGlobalStorage()
                .dataExist(TestData.class, uuid1));

        NetworkLogger.info("Testing remote pipeline:");

        assertFalse(remotePipeline
                .exist(TestData.class, uuid1)
                .join());
        assertFalse(remotePipeline
                .getLocalCache()
                .dataExist(TestData.class, uuid1));
        assertFalse(remotePipeline
                .getGlobalCache()
                .dataExist(TestData.class, uuid1));
        assertFalse(remotePipeline
                .getGlobalStorage()
                .dataExist(TestData.class, uuid1));
    }

    @Test
    public void testRemoveAndCreate1() {
        pipeline
                .loadOrCreate(TestData.class, uuid1)
                .thenApply(pipelineLock -> pipelineLock.performWriteOperation(testData1 -> testData1.testString = "Peter"))
                .join();
        pipeline
                .delete(TestData.class, uuid1)
                .join();
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

        var existsInCache = pipeline
                .getGlobalCache()
                .dataExist(OnlyLocalData.class, uuid1);
        var existsInStorage = pipeline
                .getGlobalStorage()
                .dataExist(OnlyLocalData.class, uuid1);
        assertFalse(existsInCache);
        assertFalse(existsInStorage);
    }

    @Test
    public void testOnlyCache() {
        pipeline
                .loadOrCreate(OnlyCacheData.class, uuid1)
                .join();

        var existsInCache = pipeline
                .getGlobalCache()
                .dataExist(OnlyCacheData.class, uuid1);
        var existsInStorage = pipeline
                .getGlobalStorage()
                .dataExist(OnlyCacheData.class, uuid1);

        assertTrue(existsInCache);
        assertFalse(existsInStorage);
    }

    @Test
    public void testOnlyStorage() {
        pipeline
                .loadOrCreate(OnlyStorageData.class, uuid1)
                .join();

        var existsInCache = pipeline
                .getGlobalCache()
                .dataExist(OnlyStorageData.class, uuid1);
        var existsInStorage = pipeline
                .getGlobalStorage()
                .dataExist(OnlyStorageData.class, uuid1);

        assertFalse(existsInCache);
        assertTrue(existsInStorage);
    }

    @Test
    public void createdAndLoadedOnRemotePipeline() {
        pipeline
                .loadOrCreate(LoadBeforeTest.class, uuid1)
                .join();

        var found = remotePipeline
                .getLocalCache()
                .dataExist(LoadBeforeTest.class, uuid1);
        assertTrue(found);
    }

    @Test
    public void MySQLTestCreateData() {
        mysqlNetworkParticipant
                .pipeline()
                .loadOrCreate(TestData.class, uuid1)
                .join()
                .performWriteOperation(testData1 -> testData1.testInt = 1);

        assertTrue(mysqlNetworkParticipant
                .pipeline()
                .exist(TestData.class, uuid1)
                .join());
    }

    @Test
    public void JsonTestCreateData() {
        jsonNetworkParticipant
                .pipeline()
                .loadOrCreate(TestData.class, uuid1)
                .join()
                .performWriteOperation(testData1 -> testData1.testInt = 1);

        assertTrue(jsonNetworkParticipant
                .pipeline()
                .exist(TestData.class, uuid1)
                .join());
    }

    @AfterAll
    public static void cleanUp() {
        if (testData != null)
            pipeline.delete(testData.getClass(), testData.getObjectUUID());

        pipeline
                .delete(TestData.class, uuid1)
                .join();
        pipeline
                .delete(OnlyLocalData.class, uuid1)
                .join();
        pipeline
                .delete(OnlyCacheData.class, uuid1)
                .join();
        pipeline
                .delete(OnlyStorageData.class, uuid1)
                .join();
        pipeline
                .delete(LoadBeforeTest.class, uuid1)
                .join();

        networkParticipant1.shutdown();
        networkParticipant2.shutdown();
        scheduledExecutorService.shutdownNow();
    }
}
