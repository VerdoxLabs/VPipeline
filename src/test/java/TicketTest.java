import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.NetworkParticipant;
import de.verdox.vpipeline.api.VNetwork;
import de.verdox.vpipeline.api.messaging.Transmitter;
import de.verdox.vpipeline.api.pipeline.parts.NetworkDataLockingService;
import de.verdox.vpipeline.api.pipeline.datatypes.SynchronizingService;
import de.verdox.vpipeline.api.pipeline.parts.GlobalCache;
import model.ticket.AbstractTestTicket;
import model.ticket.TestTicket;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import redis.embedded.RedisServer;

import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class TicketTest {
    public static NetworkParticipant networkParticipant1;
    public static NetworkParticipant networkParticipant2;
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
                .withMessagingService(messagingServiceBuilder -> messagingServiceBuilder.withTransmitter(Transmitter.createRedisTransmitter(false, new String[]{"redis://127.0.0.1:6379"}, "")))
                .withPipeline(pipelineBuilder -> pipelineBuilder
                        .withNetworkDataLockingService(NetworkDataLockingService.createRedis(false, new String[]{"redis://127.0.0.1:6379"}, ""))
                        .withGlobalCache(GlobalCache.createRedisCache(false, new String[]{"redis://127.0.0.1:6379"}, ""))
                        .withSynchronizingService(SynchronizingService.buildRedisService(false, new String[]{"redis://127.0.0.1:6379"}, ""))
                ).build();

        networkParticipant2 = VNetwork
                .getConstructionService()
                .createNetworkParticipant()
                .withName("s2")
                .withMessagingService(messagingServiceBuilder -> messagingServiceBuilder.withTransmitter(Transmitter.createRedisTransmitter(false, new String[]{"redis://127.0.0.1:6379"}, "")))
                .withPipeline(pipelineBuilder -> pipelineBuilder
                        .withNetworkDataLockingService(NetworkDataLockingService.createRedis(false, new String[]{"redis://127.0.0.1:6379"}, ""))
                        .withGlobalCache(GlobalCache.createRedisCache(false, new String[]{"redis://127.0.0.1:6379"}, ""))
                        .withSynchronizingService(SynchronizingService.buildRedisService(false, new String[]{"redis://127.0.0.1:6379"}, ""))
                ).build();

        networkParticipant1.messagingService().getTicketPropagator().registerTicketType("test_ticket", TestTicket.class, TestTicket::new);
        networkParticipant2.messagingService().getTicketPropagator().registerTicketType("test_ticket", TestTicket.class, TestTicket::new);

        networkParticipant1.connect();
        networkParticipant2.connect();
    }

    @Test
    public void testSimpleTicketInstruction() throws InterruptedException {
        int testInteger = 1;
        String inputString = "test";
        UUID testUUID = UUID.randomUUID();

        networkParticipant1.messagingService().getTicketPropagator().issueTicket(new TestTicket(testInteger, inputString, testUUID));
        Thread.sleep(50);
        Set<TestTicket> consumedTickets = networkParticipant2.messagingService().getTicketPropagator().consumeTicket(TestTicket.class);
        Assertions.assertEquals(1, consumedTickets.size());
        Thread.sleep(50);
        consumedTickets = networkParticipant1.messagingService().getTicketPropagator().consumeTicket(TestTicket.class);
        Assertions.assertEquals(0, consumedTickets.size());
    }

    @Test
    public void testSimpleTicketGroupInstruction() throws InterruptedException {
        int testInteger = 1;
        String inputString = "test";
        UUID testUUID = UUID.randomUUID();

        networkParticipant1.messagingService().getTicketPropagator().issueTicket(new TestTicket(testInteger, inputString, testUUID));
        Thread.sleep(50);
        Set<AbstractTestTicket> consumedTickets = networkParticipant2.messagingService().getTicketPropagator().consumeTicketGroup(AbstractTestTicket.class);
        Assertions.assertEquals(1, consumedTickets.size());
        Thread.sleep(50);
        consumedTickets = networkParticipant1.messagingService().getTicketPropagator().consumeTicketGroup(AbstractTestTicket.class);
        Assertions.assertEquals(0, consumedTickets.size());
    }

    @Test
    public void testTicketDataTransferredCorrectly() throws InterruptedException {
        int testInteger = 1;
        String testString = "test";
        UUID testUUID = UUID.randomUUID();

        networkParticipant1.messagingService().getTicketPropagator().issueTicket(new TestTicket(testInteger, testString, testUUID));
        Thread.sleep(50);
        Set<TestTicket> consumedTickets = networkParticipant2.messagingService().getTicketPropagator().consumeTicket(TestTicket.class);
        TestTicket testTicket = consumedTickets.stream().findAny().get();

        Assertions.assertEquals(testInteger, testTicket.testInteger);
        Assertions.assertEquals(testString, testTicket.testString);
        Assertions.assertEquals(testUUID, testTicket.testUUID);
    }
}
