import de.verdox.vpipeline.api.pipeline.parts.cache.local.AccessInvalidException;
import de.verdox.vpipeline.api.pipeline.parts.cache.local.DataAccess;
import de.verdox.vpipeline.api.pipeline.parts.cache.local.LockableAction;
import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.NetworkParticipant;
import de.verdox.vpipeline.api.VNetwork;
import de.verdox.vpipeline.api.pipeline.core.NetworkDataLockingService;
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
import java.util.logging.Level;

public class PipelineTests {
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
    }

    @Test
    public void testCreateExistRemoteInLocalCache() throws InterruptedException {
        UUID uuid = UUID.randomUUID();
        pipeline.loadOrCreate(TestData.class, uuid);
        Thread.sleep(50);
        boolean exist = remotePipeline.getLocalCache().dataExist(TestData.class, uuid);
        Assertions.assertTrue(exist);
    }

    @Test
    public void testCreateExistRemoteInGlobalCacheInstantly() {
        UUID uuid = UUID.randomUUID();
        pipeline.loadOrCreate(TestData.class, uuid);
        boolean exist = remotePipeline.getGlobalCache().dataExist(TestData.class, uuid);
        Assertions.assertTrue(exist);
    }

    @Test
    public void testCreateExistInRemotePipelineInstantly() {
        UUID uuid = UUID.randomUUID();
        pipeline.loadOrCreate(TestData.class, uuid);
        boolean exist = remotePipeline.exist(TestData.class, uuid);
        Assertions.assertTrue(exist);
    }

    @Test
    public void testAlterDataCheckOnRemote1() throws InterruptedException {
        UUID uuid = UUID.randomUUID();
        try (LockableAction.Write<TestData> write = pipeline.loadOrCreate(TestData.class, uuid).write()) {
            TestData testData = write.get();
            testData.testInt = 1;
            testData.save(true);
        } catch (AccessInvalidException e) {
            throw new RuntimeException(e);
        }
        Thread.sleep(50);

        try (LockableAction.Read<TestData> read = remotePipeline.load(TestData.class, uuid).read()) {
            TestData testData = read.get();
            int observedRemotely = testData.testInt;
            Assertions.assertEquals(1, observedRemotely);
        } catch (AccessInvalidException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testAlterDataCheckOnRemote2() throws InterruptedException {
        UUID uuid = UUID.randomUUID();
        DataAccess<TestData> access = pipeline.loadOrCreate(TestData.class, uuid);
        Thread.sleep(50);
        DataAccess<TestData> accessRemote = remotePipeline.load(TestData.class, uuid);
        Assertions.assertNotNull(accessRemote);
        Thread.sleep(50);
        try (LockableAction.Write<TestData> write = access.write()) {
            TestData testData = write.get();
            testData.testInt = 1;
            testData.save(true);
        }
        catch (AccessInvalidException e) {
            throw new RuntimeException(e);
        }
        try (LockableAction.Read<TestData> read = accessRemote.read()) {
            TestData testData = read.get();
            int observedRemotely = testData.testInt;
            Assertions.assertEquals(1, observedRemotely);
        }
        catch (AccessInvalidException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testAlterDataCheckOnRemote3() throws InterruptedException {
        UUID uuid = UUID.randomUUID();
        DataAccess<TestData> access = pipeline.loadOrCreate(TestData.class, uuid);
        Thread.sleep(50);
        DataAccess<TestData> accessRemote = remotePipeline.load(TestData.class, uuid);
        Assertions.assertNotNull(accessRemote);
        Thread.sleep(50);

        Thread t1 = new Thread(() -> {
            try (LockableAction.Write<TestData> write = access.write()) {
                TestData testData = write.get();
                testData.testInt += 1;
                testData.save(true);
            }
            catch (AccessInvalidException e) {
                throw new RuntimeException(e);
            }
        });

        Thread t2 = new Thread(() -> {
            try (LockableAction.Write<TestData> write = accessRemote.write()) {
                TestData testData = write.get();
                testData.testInt += 1;
                testData.save(true);
            }
            catch (AccessInvalidException e) {
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
        }
        catch (AccessInvalidException e) {
            throw new RuntimeException(e);
        }

        try (LockableAction.Read<TestData> read = accessRemote.read()) {
            TestData testData = read.get();
            int observedRemotely = testData.testInt;
            Assertions.assertEquals(2, observedRemotely);
        }
        catch (AccessInvalidException e) {
            throw new RuntimeException(e);
        }
    }
}
