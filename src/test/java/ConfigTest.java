import de.verdox.vpipeline.api.VNetwork;
import de.verdox.vpipeline.api.config.PipelineConfig;
import de.verdox.vpipeline.api.pipeline.parts.GlobalCache;
import de.verdox.vpipeline.api.pipeline.parts.GlobalStorage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import redis.embedded.RedisServer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class ConfigTest {
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
        new File("test.json").delete();
    }

    @Test
    public void testReadNetworkParticipantFromConfigWithNoErrors() throws IOException {
        Assertions.assertDoesNotThrow(() -> {
            new PipelineConfig(new File("test.json"),
                    VNetwork.getConstructionService().createNetworkParticipant()
                            .withName("pipeline")
                            .withPipeline(pipelineBuilder ->
                                    pipelineBuilder
                                            .withGlobalCache(GlobalCache.createRedisCache(false, new String[]{"redis://localhost:6379"}, ""))
                                            .withGlobalStorage(GlobalStorage.buildJsonStorage(Path.of("testStorage")))
                            )
                            .withMessagingService()
                            .build(), true)
                    .load();
        });
    }

    @Test
    public void testReadAndStartNetworkParticipantFromConfigWithNoErrors() throws IOException {
        Assertions.assertDoesNotThrow(() -> {
            new PipelineConfig(new File("test.json"),
                    VNetwork.getConstructionService().createNetworkParticipant()
                            .withName("pipeline")
                            .withPipeline(pipelineBuilder ->
                                    pipelineBuilder
                                            .withGlobalCache(GlobalCache.createRedisCache(false, new String[]{"redis://localhost:6379"}, ""))
                                            .withGlobalStorage(GlobalStorage.buildJsonStorage(Path.of("testStorage")))
                            )
                            .withMessagingService()
                            .build(), true)
                    .load()
                    .connect();
        });
    }

}
