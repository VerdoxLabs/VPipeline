package de.verdox.vpipeline.impl.util;

import de.verdox.vpipeline.api.Connection;
import de.verdox.vpipeline.api.pipeline.core.SystemPart;
import de.verdox.vpipeline.api.pipeline.datatypes.IPipelineData;
import de.verdox.vpipeline.api.util.AnnotationResolver;
import de.verdox.vserializer.SerializableField;
import de.verdox.vserializer.json.JsonSerializer;
import de.verdox.vserializer.json.JsonSerializerBuilder;
import org.jetbrains.annotations.NotNull;
import org.redisson.Redisson;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.codec.SerializationCodec;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class RedisConnection implements SystemPart, Connection {
    public static final JsonSerializer<RedisConnection> SERIALIZER = JsonSerializerBuilder.create("redis_connection", RedisConnection.class)
            .constructor(
                    new SerializableField<>("clusterMode", JsonSerializer.Primitive.BOOLEAN, RedisConnection::isClusterMode),
                    new SerializableField<>("addressArray", JsonSerializer.Collection.create(JsonSerializer.Primitive.STRING, ArrayList::new), redisConnection -> Arrays.stream(redisConnection.addressArray).toList()),
                    new SerializableField<>("redisPassword", JsonSerializer.Primitive.STRING, RedisConnection::getRedisPassword),
                    (aBoolean, list, s) -> new RedisConnection(aBoolean, list.toArray(new String[0]), s)
            )
            .build();
    private final Config config;
    protected RedissonClient redissonClient;
    private final boolean clusterMode;
    private final @NotNull String[] addressArray;
    private final String redisPassword;

    public RedisConnection(boolean clusterMode, @NotNull String[] addressArray, String redisPassword) {
        this.clusterMode = clusterMode;
        this.addressArray = addressArray;
        this.redisPassword = redisPassword;
        Objects.requireNonNull(addressArray, "addressArray can't be null!");
        Objects.requireNonNull(redisPassword, "redisPassword can't be null!");
        if (addressArray.length == 0)
            throw new IllegalArgumentException("Address Array empty");
        config = new Config();

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

    public boolean isClusterMode() {
        return clusterMode;
    }

    public String[] getAddressArray() {
        return addressArray;
    }

    public String getRedisPassword() {
        return redisPassword;
    }

    @Override
    public void connect() {
        this.redissonClient = Redisson.create(config);
    }

    @Override
    public void disconnect() {
        this.redissonClient.shutdown();
    }
}
