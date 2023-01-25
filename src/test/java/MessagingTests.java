import de.verdox.vpipeline.api.NetworkParticipant;
import de.verdox.vpipeline.api.VNetwork;
import de.verdox.vpipeline.api.messaging.MessagingService;
import de.verdox.vpipeline.api.messaging.instruction.types.Update;
import io.netty.util.concurrent.DefaultThreadFactory;
import model.messages.TestQuery;
import model.messages.TestUpdate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class MessagingTests {

    public static NetworkParticipant networkParticipant1;
    public static NetworkParticipant networkParticipant2;
    public static MessagingService messagingService1;
    public static MessagingService messagingService2;

    public static final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(4, new DefaultThreadFactory("VPipeline-ThreadPool [MessagingTests]"));
    ;

    @BeforeEach
    public void testAnnounce() {
    }

    @BeforeAll
    public static void setupPipeline() throws InterruptedException {
        networkParticipant1 = createTestService("server1");
        networkParticipant2 = createTestService("server2");
        messagingService1 = networkParticipant1.messagingService();
        messagingService2 = networkParticipant2.messagingService();
        Thread.sleep(1000);
    }

    @Test
    public void testUpdate1() {
        messagingService1.sendInstruction(TestUpdate.class, testUpdate -> testUpdate.name = "Hans")
                         .waitForValue(updateCompletion -> updateCompletion.equals(Update.UpdateCompletion.DONE));
    }


    @AfterAll
    public static void cleanUp() {
        networkParticipant1.shutdown();
        networkParticipant2.shutdown();
        scheduledExecutorService.shutdown();
    }

    public static NetworkParticipant createTestService(String identifier) {
        var participant = VNetwork
                .getConstructionService()
                .createNetworkParticipant()
                .withName(identifier)
                .withExecutorService(scheduledExecutorService)
                .withMessagingService(messagingServiceBuilder -> messagingServiceBuilder
                        .useRedisTransmitter(false, new String[]{"redis://localhost:6379"}, "")).build();

        var service = participant.messagingService();
        service
                .getMessageFactory()
                .registerInstructionType(0, TestUpdate.class, () -> new TestUpdate(UUID.randomUUID()));
        service.getMessageFactory().registerInstructionType(1, TestQuery.class, () -> new TestQuery(UUID.randomUUID()));
        return participant;
    }

}
