import de.verdox.vpipeline.api.VNetwork;
import de.verdox.vpipeline.api.pipeline.datatypes.SynchronizingService;
import de.verdox.vpipeline.api.pipeline.datatypes.customtypes.DataReference;
import de.verdox.vpipeline.api.pipeline.parts.GlobalCache;
import de.verdox.vpipeline.api.pipeline.parts.GlobalStorage;
import model.TestData;
import model.TestPing;

import java.io.Serializable;
import java.util.UUID;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        testDataConcurrency();
    }

    public static void testPingsConcurrent() {

        new Thread(() -> {
            var messagingService1 = VNetwork
                    .getConstructionService()
                    .createMessagingService()
                    .withIdentifier("server1")
                    .useRedisTransmitter(false, new String[]{"redis://localhost:6379"}, "")
                    .buildMessagingService();

            messagingService1.getMessageFactory().registerInstructionType(0, TestPing.class);

            //messagingService1.shutdown();
        }).start();

        new Thread(() -> {
            var messagingService2 = VNetwork
                    .getConstructionService()
                    .createMessagingService()
                    .withIdentifier("server2")
                    .useRedisTransmitter(false, new String[]{"redis://localhost:6379"}, "")
                    .buildMessagingService();

            messagingService2.getMessageFactory().registerInstructionType(0, TestPing.class);

            var tPing = new TestPing(UUID.randomUUID()).withData(1);

            messagingService2.sendInstruction(tPing);
            //messagingService2.shutdown();
        }).start();
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

    public static void testDataConcurrency() {
        UUID uuid = UUID.randomUUID();

        var pipeline = VNetwork
                .getConstructionService()
                .createPipeline()
                .withGlobalCache(GlobalCache.createRedisCache(false, new String[]{"redis://localhost:6379"}, ""))
                .withSynchronizingService(SynchronizingService.buildRedisService(false, new String[]{"redis://localhost:6379"}, ""))
                .withGlobalStorage(GlobalStorage.buildMongoDBStorage("127.0.0.1", "vPipelineTest", 27017, "", ""))
                .buildPipeline();

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
        }).start();

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
}
