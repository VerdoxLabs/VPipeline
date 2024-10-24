import de.verdox.vpipeline.api.pipeline.parts.cache.local.AccessInvalidException;
import de.verdox.vpipeline.api.pipeline.parts.cache.local.DataAccess;
import de.verdox.vpipeline.api.pipeline.parts.cache.local.DataSubscriber;
import de.verdox.vpipeline.api.pipeline.parts.cache.local.LockableAction;
import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.NetworkParticipant;
import de.verdox.vpipeline.api.VNetwork;
import de.verdox.vpipeline.api.pipeline.parts.NetworkDataLockingService;
import de.verdox.vpipeline.api.pipeline.core.Pipeline;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import de.verdox.vpipeline.api.pipeline.datatypes.SynchronizingService;
import de.verdox.vpipeline.api.pipeline.parts.GlobalCache;
import model.data.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import redis.embedded.RedisServer;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class PipelineSyncTest {
    public static NetworkParticipant networkParticipant1;
    public static NetworkParticipant networkParticipant2;
    public static Pipeline pipeline;
    public static Pipeline remotePipeline;
    private static RedisServer redisServer = null;

    @BeforeAll
    public static void startRedis() {
        if (redisServer == null) {
            redisServer = RedisServer.builder()
                    .port(6379)
                    .setting("bind 127.0.0.1") // secure + prevents popups on Windows
                    .setting("maxmemory 128M")
                    .setting("timeout 100000")
                    .build();
            redisServer.start();
        }
    }

    @AfterAll
    public static void stopRedis() {
        if (redisServer != null) {
            redisServer.stop();
            redisServer = null;
        }
    }

    @BeforeAll
    public static void setupPipeline() {
        NetworkLogger.debugMode.setDebugMode(true);
        NetworkLogger.setLevel(Level.ALL);

        networkParticipant1 = VNetwork
                .getConstructionService()
                .createNetworkParticipant()
                .withName("s1")
                //.withExecutorService(scheduledExecutorService)
                .withPipeline(pipelineBuilder -> pipelineBuilder
                        .withNetworkDataLockingService(NetworkDataLockingService.createRedis(false, new String[]{"redis://127.0.0.1:6379"}, ""))
                        .withGlobalCache(GlobalCache.createRedisCache(false, new String[]{"redis://127.0.0.1:6379"}, ""))
                        .withSynchronizingService(SynchronizingService.buildRedisService(false, new String[]{"redis://127.0.0.1:6379"}, ""))
                ).build();

        networkParticipant2 = VNetwork
                .getConstructionService()
                .createNetworkParticipant()
                .withName("s2")
                //.withExecutorService(scheduledExecutorService)
                .withPipeline(pipelineBuilder -> pipelineBuilder
                        .withNetworkDataLockingService(NetworkDataLockingService.createRedis(false, new String[]{"redis://127.0.0.1:6379"}, ""))
                        .withGlobalCache(GlobalCache.createRedisCache(false, new String[]{"redis://127.0.0.1:6379"}, ""))
                        .withSynchronizingService(SynchronizingService.buildRedisService(false, new String[]{"redis://127.0.0.1:6379"}, ""))
                ).build();

        pipeline = networkParticipant1.pipeline();
        remotePipeline = networkParticipant2.pipeline();

        Class<? extends IPipelineData>[] types = new Class[]{TestData.class, OnlyLocalData.class, OnlyCacheData.class, OnlyStorageData.class, LoadBeforeTest.class};

        for (Class<? extends IPipelineData> type : types) {
            pipeline.getDataRegistry().registerType(type);
            remotePipeline.getDataRegistry().registerType(type);
        }
        networkParticipant1.connect();
        networkParticipant2.connect();
    }

    /**
     * Checks if data that was created on one pipeline object is loaded into the remote local cache pipeline
     * A thread sleep is used since the local redis server does not run on the same thread which is why the remote pipeline will not have received the creation call yet.
     *
     * @throws InterruptedException if the test is interrupted
     */
    @Test
    public void testCreateExistRemoteInLocalCache() throws InterruptedException {
        UUID uuid = UUID.randomUUID();
        pipeline.loadOrCreate(TestData.class, uuid);
        Thread.sleep(50);
        boolean exist = remotePipeline.getLocalCache().dataExist(TestData.class, uuid);
        Assertions.assertTrue(exist);
    }

    /**
     * Checks if data that was created on one pipeline object is recognized in the remote global cache object immediately.
     */
    @Test
    public void testCreateExistRemoteInGlobalCacheInstantly() {
        UUID uuid = UUID.randomUUID();
        pipeline.loadOrCreate(TestData.class, uuid);
        boolean exist = remotePipeline.getGlobalCache().dataExist(TestData.class, uuid);
        Assertions.assertTrue(exist);
    }

    /**
     * Checks if data that was created on one pipeline object is recognized on a remote pipeline object immediately.
     */
    @Test
    public void testCreateExistInRemotePipelineInstantly() {
        UUID uuid = UUID.randomUUID();
        pipeline.loadOrCreate(TestData.class, uuid);
        boolean exist = remotePipeline.exist(TestData.class, uuid);
        Assertions.assertTrue(exist);
    }

    /**
     * Checks if data that was created and updated has the correct state on the remote pipeline
     */
    @Test
    public void testAlterDataCheckOnRemote1() {
        UUID uuid = UUID.randomUUID();
        try (LockableAction.Write<TestData> write = pipeline.loadOrCreate(TestData.class, uuid).write()) {
            TestData testData = write.get();
            testData.testInt = 1;
            testData.save(true);
        } catch (AccessInvalidException e) {
            throw new RuntimeException(e);
        }

        try (LockableAction.Read<TestData> read = remotePipeline.load(TestData.class, uuid).read()) {
            TestData testData = read.get();
            int observedRemotely = testData.testInt;
            Assertions.assertEquals(1, observedRemotely);
        } catch (AccessInvalidException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks if data that was created and updated has the correct state on the remote pipeline
     */
    @Test
    public void testAlterDataCheckOnRemote2() {
        UUID uuid = UUID.randomUUID();
        DataAccess<TestData> access = pipeline.loadOrCreate(TestData.class, uuid);
        DataAccess<TestData> accessRemote = remotePipeline.loadOrCreate(TestData.class, uuid);
        Assertions.assertNotNull(accessRemote);
        try (LockableAction.Write<TestData> write = access.write()) {
            TestData testData = write.get();
            testData.testInt = 1;
            testData.save(true);
        } catch (AccessInvalidException e) {
            throw new RuntimeException(e);
        }
        try (LockableAction.Read<TestData> read = accessRemote.read()) {
            TestData testData = read.get();
            int observedRemotely = testData.testInt;
            Assertions.assertEquals(1, observedRemotely);
        } catch (AccessInvalidException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks if data that was updated in parallel by two pipelines has the correct state on both pipelines after the operation
     */
    @Test
    public void testAlterDataByTwoParties() throws InterruptedException {
        UUID uuid = UUID.randomUUID();
        DataAccess<TestData> access = pipeline.loadOrCreate(TestData.class, uuid);
        DataAccess<TestData> accessRemote = remotePipeline.loadOrCreate(TestData.class, uuid);
        Assertions.assertNotNull(accessRemote);

        Thread t1 = new Thread(() -> {
            try (LockableAction.Write<TestData> write = access.write()) {
                TestData testData = write.get();
                testData.testInt += 1;
                testData.save(true);
            } catch (AccessInvalidException e) {
                throw new RuntimeException(e);
            }
        });

        Thread t2 = new Thread(() -> {
            try (LockableAction.Write<TestData> write = accessRemote.write()) {
                TestData testData = write.get();
                testData.testInt += 1;
                testData.save(true);
            } catch (AccessInvalidException e) {
                throw new RuntimeException(e);
            }
        });

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        try (LockableAction.Read<TestData> read = access.read()) {
            TestData testData = read.get();
            int observedRemotely = testData.testInt;
            Assertions.assertEquals(2, observedRemotely);
        } catch (AccessInvalidException e) {
            throw new RuntimeException(e);
        }

        try (LockableAction.Read<TestData> read = accessRemote.read()) {
            TestData testData = read.get();
            int observedRemotely = testData.testInt;
            Assertions.assertEquals(2, observedRemotely);
        } catch (AccessInvalidException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks if only one create call is recognized by the network. After the creation both pipelines must have the same object state
     */
    @Test
    public void testCreateTwoTimes() {
        UUID uuid = UUID.randomUUID();

        pipeline.loadOrCreate(TestData.class, uuid, testData -> testData.testInt = 1);
        remotePipeline.loadOrCreate(TestData.class, uuid, testData -> testData.testInt = 2);

        try (LockableAction.Read<TestData> read1 = pipeline.load(TestData.class, uuid).read(); LockableAction.Read<TestData> read2 = remotePipeline.load(TestData.class, uuid).read()) {
            Assertions.assertEquals(read1.get().testInt, read2.get().testInt);
            Assertions.assertEquals(1, read1.get().testInt);
        } catch (AccessInvalidException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Two pipelines create new data in parallel with the same type and uuid. It is random which creation call comes first.
     * Thus, we only do a metamorphic test to check if the two pipelines read the same value
     */
    @Test
    public void testParallelCreateMetamorphic() throws InterruptedException {
        UUID uuid = UUID.randomUUID();

        Thread t1 = new Thread(() -> {
            pipeline.loadOrCreate(TestData.class, uuid, testData -> testData.testInt = 1);
        });

        Thread t2 = new Thread(() -> {
            pipeline.loadOrCreate(TestData.class, uuid, testData -> testData.testInt = 2);
        });

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        try (LockableAction.Read<TestData> read1 = pipeline.load(TestData.class, uuid).read(); LockableAction.Read<TestData> read2 = remotePipeline.load(TestData.class, uuid).read()) {
            Assertions.assertEquals(read1.get().testInt, read2.get().testInt);
        } catch (AccessInvalidException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * A DataAccess object exists even after the data was deleted from the pipeline.
     * An operation on this data access object should fail at this point with an exception.
     */
    @Test
    public void testThrowsExceptionWhenDataIsAlreadyDeleted() {
        UUID uuid = UUID.randomUUID();

        pipeline.loadOrCreate(TestData.class, uuid, testData -> testData.testInt = 1);
        DataAccess<TestData> remoteAccess = remotePipeline.loadOrCreate(TestData.class, uuid);
        pipeline.delete(TestData.class, uuid);

        Assertions.assertThrows(AccessInvalidException.class, () -> {
            try (LockableAction.Read<TestData> read = remoteAccess.read()) {
                read.get();
            }
        }, "The DataAccess should throw an AccessInvalidException because the data was already deleted");
    }

    /**
     * Data is deleted. After that a pipeline tries to load the data to create a data access object.
     * The data access object should be null at this point.
     */
    @Test
    public void testNoAccessCreatedWhenDataWasDeleted() {
        UUID uuid = UUID.randomUUID();

        pipeline.loadOrCreate(TestData.class, uuid, testData -> testData.testInt = 1);
        remotePipeline.load(TestData.class, uuid);
        pipeline.delete(TestData.class, uuid);
        DataAccess<TestData> remoteAccess = remotePipeline.load(TestData.class, uuid);
        Assertions.assertNull(remoteAccess);
    }

    /**
     * A {@link DataSubscriber} subscribes onto a {@link DataAccess} object.
     * The data is changed by the other pipeline. The {@link DataSubscriber} will receive the updated value accordingly
     * @throws InterruptedException
     */
    @Test
    public void testDataSubscribersBeingNotified() throws InterruptedException {
        UUID uuid = UUID.randomUUID();

        DataAccess<TestData> access = pipeline.loadOrCreate(TestData.class, uuid, testData -> testData.testInt = 1);

        AtomicInteger container = new AtomicInteger(0);
        access.subscribe(DataSubscriber.observeNumber(testData -> testData.testInt, container::set, 0));

        DataAccess<TestData> remoteAccess = remotePipeline.loadOrCreate(TestData.class, uuid);
        try (LockableAction.Write<TestData> write = remoteAccess.write()) {
            TestData testData = write.get();
            testData.testInt += 1;
        } catch (AccessInvalidException e) {
            throw new RuntimeException(e);
        }
        Thread.sleep(50);

        Assertions.assertEquals(2, container.get());
    }

    @Test
    public void testSubscriberNotifiedWhenCreatedBeforeDataExists() throws InterruptedException {
        AtomicInteger container = new AtomicInteger(0);
        UUID uuid = UUID.randomUUID();

        pipeline.subscribe(TestData.class, uuid, DataSubscriber.observeNumber(testData -> testData.testInt, container::set, 0));

        pipeline.loadOrCreate(TestData.class, uuid, testData -> testData.testInt = 1);
        DataAccess<TestData> remoteAccess = remotePipeline.loadOrCreate(TestData.class, uuid);
        try (LockableAction.Write<TestData> write = remoteAccess.write()) {
            TestData testData = write.get();
            testData.testInt += 1;
        } catch (AccessInvalidException e) {
            throw new RuntimeException(e);
        }
        Thread.sleep(50);

        Assertions.assertEquals(2, container.get());
    }

    @Test
    public void testSubscriberDeletedAndNotNotified() throws InterruptedException {
        AtomicInteger container = new AtomicInteger(0);
        UUID uuid = UUID.randomUUID();
        DataSubscriber<TestData, Integer> dataSubscriber = DataSubscriber.observeNumber(testData -> testData.testInt, container::set, 0);
        pipeline.subscribe(TestData.class, uuid, dataSubscriber);
        pipeline.loadOrCreate(TestData.class, uuid, testData -> testData.testInt = 1);
        pipeline.removeSubscriber(dataSubscriber);

        DataAccess<TestData> remoteAccess = remotePipeline.loadOrCreate(TestData.class, uuid);
        try (LockableAction.Write<TestData> write = remoteAccess.write()) {
            TestData testData = write.get();
            testData.testInt += 1;
        } catch (AccessInvalidException e) {
            throw new RuntimeException(e);
        }
        Thread.sleep(50);

        Assertions.assertEquals(1, container.get());
    }

    @Test
    public void testRemoteSubscribersGettingValues1() throws InterruptedException {
        AtomicInteger container = new AtomicInteger(0);
        UUID uuid = UUID.randomUUID();
        DataSubscriber<TestData, Integer> dataSubscriber = DataSubscriber.observeNumber(testData -> testData.testInt, container::set, 0);

        remotePipeline.subscribe(TestData.class, uuid, dataSubscriber);
        pipeline.loadOrCreate(TestData.class, uuid, testData -> testData.testInt = 1);

        DataAccess<TestData> remoteAccess = remotePipeline.loadOrCreate(TestData.class, uuid);
        try (LockableAction.Write<TestData> write = remoteAccess.write()) {
            TestData testData = write.get();
            testData.testInt += 1;
        } catch (AccessInvalidException e) {
            throw new RuntimeException(e);
        }
        Thread.sleep(50);

        Assertions.assertEquals(2, container.get());
    }

    @Test
    public void testRemoteSubscribersGettingValues2() throws InterruptedException {
        AtomicInteger container = new AtomicInteger(0);
        UUID uuid = UUID.randomUUID();
        DataSubscriber<TestData, Integer> dataSubscriber = DataSubscriber.observeNumber(testData -> testData.testInt, container::set, 0);

        pipeline.loadOrCreate(TestData.class, uuid, testData -> testData.testInt = 1);
        remotePipeline.subscribe(TestData.class, uuid, dataSubscriber);

        DataAccess<TestData> remoteAccess = remotePipeline.loadOrCreate(TestData.class, uuid);
        try (LockableAction.Write<TestData> write = remoteAccess.write()) {
            TestData testData = write.get();
            testData.testInt += 1;
        } catch (AccessInvalidException e) {
            throw new RuntimeException(e);
        }
        Thread.sleep(50);

        Assertions.assertEquals(2, container.get());
    }
}
