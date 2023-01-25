import de.verdox.vpipeline.api.VNetwork;
import de.verdox.vpipeline.api.pipeline.datatypes.SynchronizingService;
import de.verdox.vpipeline.api.pipeline.parts.GlobalCache;
import de.verdox.vpipeline.api.pipeline.parts.GlobalStorage;
import model.data.TestData;
import model.messages.TestQuery;
import model.messages.TestUpdate;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        //asyncTest();

        var service1 = MessagingTests.createTestService("server1");
        var service2 = MessagingTests.createTestService("server2");
    }

/*    public static void testDataReference() throws InterruptedException {
        UUID uuid = UUID.randomUUID();
        UUID uuid1 = UUID.randomUUID();

        var pipeline = VNetwork
                .getConstructionService()
                .createPipeline()
                .withGlobalCache(GlobalCache.createRedisCache(false, new String[]{"redis://localhost:6379"}, ""))
                .withSynchronizingService(SynchronizingService.buildRedisService(false, new String[]{"redis://localhost:6379"}, ""))
                .withGlobalStorage(GlobalStorage.buildMongoDBStorage("127.0.0.1", "vPipelineTest", 27017, "", ""))
                .buildPipeline();

        pipeline.getDataRegistry().registerType(TestData.class);

        var result = pipeline.loadOrCreate(TestData.class, uuid);

        result.thenAccept(testDataSynchronizedAccess ->
                testDataSynchronizedAccess
                        .write(testData -> pipeline
                                        .loadOrCreate(TestData.class, uuid1)
                                        .thenApply(testDataSynchronizedAccess1 -> testData.referenceSet.add(DataReference.of(testDataSynchronizedAccess1)))
                                , true)
                        .delete(true)
                        .write(testData -> testData.testInt += 1, true)
        );

        Thread.sleep(1000 * 2);
        pipeline.shutdown();
    }*/

/*    public static void testDataConcurrency() {
        UUID uuid = UUID.randomUUID();

        var networkParticipant = VNetwork
                .getConstructionService()
                .createNetworkParticipant()
                .withPipeline(pipelineBuilder -> pipelineBuilder
                        .withGlobalCache(GlobalCache.createRedisCache(false, new String[]{"redis://localhost:6379"}, ""))
                        .withSynchronizingService(SynchronizingService.buildRedisService(false, new String[]{"redis://localhost:6379"}, ""))
                        .withGlobalStorage(GlobalStorage.buildMongoDBStorage("127.0.0.1", "vPipelineTest", 27017, "", "")))
                .build();
        var pipeline = networkParticipant.pipeline();

        pipeline.getDataRegistry().registerType(TestData.class);

        var remotePipeline = VNetwork
                .getConstructionService()
                .createPipeline()
                .withGlobalCache(GlobalCache.createRedisCache(false, new String[]{"redis://localhost:6379"}, ""))
                .withSynchronizingService(SynchronizingService.buildRedisService(false, new String[]{"redis://localhost:6379"}, ""))
                .withGlobalStorage(GlobalStorage.buildMongoDBStorage("127.0.0.1", "vPipelineTest", 27017, "", ""))
                .buildPipeline();

        remotePipeline.getDataRegistry().registerType(TestData.class);

        new Thread(() -> {
            var data = pipeline.loadOrCreate(TestData.class, uuid);

            data.thenApply(testDataPipelineLock -> testDataPipelineLock.performWriteOperation(testData -> {
                for (int i = 0; i < 100; i++)
                    testData.testInt += 1;
            }, true));

            data.thenApply(testDataPipelineLock -> {
                for (int i = 0; i < 100; i++) {
                    testDataPipelineLock.performWriteOperation(testData -> testData.testInt += 1, true);
                }
                return testDataPipelineLock;
            });

            try {
                Thread.sleep(1000 * 2);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            data.thenApply(testDataPipelineLock -> testDataPipelineLock.performReadOperation(testData -> System.out.println(testData.testInt)));

            pipeline.shutdown();
        }).start();

        new Thread(() -> {
            var data = pipeline.loadOrCreate(TestData.class, uuid);

            data.thenApply(testDataPipelineLock -> testDataPipelineLock.performWriteOperation(testData -> {
                for (int i = 0; i < 100; i++)
                    testData.testInt += 1;
            }, true));

            try {
                Thread.sleep(1000 * 2);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            data.thenApply(testDataPipelineLock -> testDataPipelineLock.performReadOperation(testData -> System.out.println(testData.testInt)));

            pipeline.shutdown();
        }).start();

        new Thread(() -> {
            var data = remotePipeline.loadOrCreate(TestData.class, uuid);

            data.thenApply(testDataPipelineLock -> testDataPipelineLock.performWriteOperation(testData -> {
                for (int i = 0; i < 100; i++)
                    testData.testInt += 1;
            }, true));

            try {
                Thread.sleep(1000 * 2);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            data.thenApply(testDataPipelineLock -> testDataPipelineLock.performReadOperation(testData -> System.out.println(testData.testInt)));

            remotePipeline.shutdown();
        }).start();*/

/*        new Thread(() -> {
            var data = pipeline.loadOrCreate(TestData.class, uuid);

            data.whenComplete((testDataSynchronizedAccess, throwable) -> {
                testDataSynchronizedAccess.write(testData -> {
                    System.out.println("[2] Loaded: " + testData.testInt + " [" + testData + "]");
                    testData.testInt += 1;
                    testData.save(true);
                    System.out.println("[2] Saved: " + testData.testInt);
                });
            });
            pipeline.shutdown();
        }).start();*/
}

/*    public static void asyncTest() throws InterruptedException {
        var pipeline = VNetwork
                .getConstructionService()
                .createPipeline()
                .withGlobalCache(GlobalCache.createRedisCache(false, new String[]{"redis://localhost:6379"}, ""))
                .withSynchronizingService(SynchronizingService.buildRedisService(false, new String[]{"redis://localhost:6379"}, ""))
                .withGlobalStorage(GlobalStorage.buildMongoDBStorage("127.0.0.1", "vPipelineTest", 27017, "", ""))
                .buildPipeline();

        var remotePipeline = VNetwork
                .getConstructionService()
                .createPipeline()
                .withGlobalCache(GlobalCache.createRedisCache(false, new String[]{"redis://localhost:6379"}, ""))
                .withSynchronizingService(SynchronizingService.buildRedisService(false, new String[]{"redis://localhost:6379"}, ""))
                .withGlobalStorage(GlobalStorage.buildMongoDBStorage("127.0.0.1", "vPipelineTest", 27017, "", ""))
                .buildPipeline();

        var uuid = UUID.randomUUID();

        pipeline.loadOrCreate(TestData.class, uuid)
                .thenApply(pipelineLock -> pipelineLock.performWriteOperation(testData -> testData.testInt += 1))
                .thenApply(pipelineLock -> pipelineLock.performWriteOperation(testData -> testData.testString = "hallo"))
                .join();

        var t1 = new Thread(() -> {
            remotePipeline.loadOrCreate(TestData.class, uuid)
                          .thenApply(pipelineLock -> pipelineLock.performWriteOperation(testData -> testData.testInt += 1))
                          .thenApply(pipelineLock -> pipelineLock.performWriteOperation(testData -> testData.testString = "hallo"))
                          .join();
        });
        t1.start();
        t1.join();

        pipeline.delete(TestData.class, uuid).join();


        pipeline.shutdown();
        remotePipeline.shutdown();
    }*/
/*}*/
