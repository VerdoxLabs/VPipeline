package de.verdox.vpipeline.impl.util;

import de.verdox.vpipeline.api.pipeline.core.SystemPart;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import de.verdox.vpipeline.api.util.AnnotationResolver;
import org.jetbrains.annotations.NotNull;
import org.redisson.Redisson;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.codec.MarshallingCodec;
import org.redisson.codec.SerializationCodec;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;

import java.util.Objects;

public class RedisConnection implements SystemPart {
    protected final RedissonClient redissonClient;

    public RedisConnection(boolean clusterMode, @NotNull String[] addressArray, String redisPassword) {
        Objects.requireNonNull(addressArray, "addressArray can't be null!");
        Objects.requireNonNull(redisPassword, "redisPassword can't be null!");
        if (addressArray.length == 0)
            throw new IllegalArgumentException("Address Array empty");
        Config config = new Config();
        if (clusterMode) {
            ClusterServersConfig clusterServersConfig = config.useClusterServers();
            clusterServersConfig.addNodeAddress(addressArray);

            if (!redisPassword.isEmpty())
                clusterServersConfig.addNodeAddress(addressArray).setPassword(redisPassword);
            else
                clusterServersConfig.addNodeAddress(addressArray);
        } else {
            SingleServerConfig singleServerConfig = config.useSingleServer();
            singleServerConfig.setSubscriptionsPerConnection(30);

            if (!redisPassword.isEmpty())
                singleServerConfig.setAddress(addressArray[0]).setPassword(redisPassword);
            else
                singleServerConfig.setAddress(addressArray[0]);
        }
        config.setNettyThreads(4);
        config.setThreads(4);
        this.redissonClient = Redisson.create(config);
    }

    public RTopic getTopic(String prefix, @NotNull Class<? extends IPipelineData> dataClass) {
        Objects.requireNonNull(dataClass, "dataClass can't be null!");
        String key = prefix + "DataTopic:" + AnnotationResolver.getDataStorageClassifier(dataClass) + "_" + AnnotationResolver.getDataStorageIdentifier(dataClass);
        return redissonClient.getTopic(key, new SerializationCodec());
    }

    public RedissonClient getRedissonClient() {
        return redissonClient;
    }

    @Override
    public void shutdown() {
        this.redissonClient.shutdown();
    }
}
