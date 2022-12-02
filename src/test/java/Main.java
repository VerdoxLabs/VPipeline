import de.verdox.vpipeline.api.VNetwork;
import de.verdox.vpipeline.api.pipeline.datatypes.SynchronizingService;
import de.verdox.vpipeline.api.pipeline.parts.GlobalCache;
import de.verdox.vpipeline.api.pipeline.parts.GlobalStorage;
import model.TestData;
import model.TestPing;

import java.util.UUID;

public class Main {
    public static void main(String[] args) {
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

            data.whenComplete((testDataSynchronizedAccess, throwable) -> {
                for (int i = 0; i < 100; i++)
                    testDataSynchronizedAccess.write(testData -> testData.testInt += 1, true);
            });

            try {
                Thread.sleep(1000 * 2);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            data.whenComplete((testDataSynchronizedAccess, throwable) -> testDataSynchronizedAccess.read(testData -> System.out.println(testData.testInt)));

            pipeline.shutdown();
        }).start();

        new Thread(() -> {
            var data = pipeline.loadOrCreate(TestData.class, uuid);

            data.whenCompleteAsync((testDataSynchronizedAccess, throwable) -> {
                for (int i = 0; i < 100; i++)
                    testDataSynchronizedAccess.write(testData -> testData.testInt += 1, true);
            });

            try {
                Thread.sleep(1000 * 2);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            data.whenComplete((testDataSynchronizedAccess, throwable) -> testDataSynchronizedAccess.read(testData -> System.out.println(testData.testInt)));

            pipeline.shutdown();
        }).start();

        new Thread(() -> {
            var data = remotePipeline.loadOrCreate(TestData.class, uuid);

            data.whenComplete((testDataSynchronizedAccess, throwable) -> {
                for (int i = 0; i < 100; i++)
                    testDataSynchronizedAccess.write(testData -> testData.testInt += 1, true);
            });
            try {
                Thread.sleep(1000 * 2);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            data.whenComplete((testDataSynchronizedAccess, throwable) -> testDataSynchronizedAccess.read(testData -> System.out.println(testData.testInt)));

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
