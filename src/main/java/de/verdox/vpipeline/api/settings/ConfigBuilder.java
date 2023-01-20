package de.verdox.vpipeline.api.settings;

import org.simpleyaml.configuration.file.YamlConfiguration;
import org.simpleyaml.configuration.serialization.ConfigurationSerializable;
import org.simpleyaml.configuration.serialization.ConfigurationSerialization;

import java.io.File;
import java.io.IOException;

public class ConfigBuilder {
    private final File file;
    private final YamlConfiguration config;

    private ConfigBuilder(File file) {
        this.file = file;
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public static ConfigBuilder create(String fileName) {
        File file = new File(fileName);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                // handle error
            }
        }
        return new ConfigBuilder(file);
    }

    public ConfigBuilder setIfNotExists(String path, Object value) {
        if (!config.contains(path)) {
            config.set(path, value);
        }
        return this;
    }

    public void save() throws IOException {
        config.save(file);
    }

    public PluginConfig build() {
        try {
            save();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new PluginConfig(config);
    }

    public static class PluginConfig {
        private final YamlConfiguration config;

        private PluginConfig(YamlConfiguration config) {
            this.config = config;
        }

        public <T> T getValue(String path, Class<T> type) {
            return type.cast(config.get(path));
        }
    }

    public static void registerSerializableType(Class<? extends ConfigurationSerializable> type) {
        ConfigurationSerialization.registerClass(type, "");
    }
}
