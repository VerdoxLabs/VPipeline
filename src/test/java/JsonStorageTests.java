import de.verdox.vpipeline.api.NetworkParticipant;
import de.verdox.vpipeline.api.VNetwork;
import de.verdox.vpipeline.api.pipeline.core.NetworkDataLockingService;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import de.verdox.vpipeline.api.pipeline.datatypes.SynchronizingService;
import de.verdox.vpipeline.api.pipeline.parts.GlobalCache;
import de.verdox.vpipeline.api.pipeline.parts.GlobalStorage;
import model.data.*;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

public class JsonStorageTests {
    public static NetworkParticipant networkParticipant;
    @BeforeAll
    public static void setup() throws IOException {

        GlobalStorage globalStorage = GlobalStorage.buildJsonStorage(Path.of("storage"));
        networkParticipant = VNetwork
                .getConstructionService()
                .createNetworkParticipant()
                .withName("s1")
                //.withExecutorService(scheduledExecutorService)
                .withPipeline(pipelineBuilder -> pipelineBuilder
                        .withNetworkDataLockingService(NetworkDataLockingService.createDummy())
                        .withGlobalStorage(globalStorage)
                ).build();
        Class<? extends IPipelineData>[] types = new Class[]{TestData.class, OnlyLocalData.class, OnlyCacheData.class, OnlyStorageData.class, LoadBeforeTest.class};

        for (Class<? extends IPipelineData> type : types) {
            networkParticipant.pipeline().getDataRegistry().registerType(type);
        }
    }

    @AfterAll
    public static void cleanUp() throws IOException {
        FileUtils.deleteDirectory(Path.of("storage").toFile());
    }

    @Test
    public void testInsert(){
        UUID uuid = UUID.randomUUID();
        networkParticipant.pipeline().loadOrCreate(TestData.class, uuid);
        Assertions.assertTrue(networkParticipant.pipeline().exist(TestData.class, uuid));
    }

    @Test
    public void testRemove(){
        UUID uuid = UUID.randomUUID();
        networkParticipant.pipeline().loadOrCreate(TestData.class, uuid);
        networkParticipant.pipeline().delete(TestData.class, uuid);
        Assertions.assertFalse(networkParticipant.pipeline().exist(TestData.class, uuid));
    }

}
