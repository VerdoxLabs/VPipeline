package de.verdox.vpipeline.api.pipeline.parts.storage;

import com.google.gson.GsonBuilder;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.verdox.vpipeline.api.NetworkLogger;
import de.verdox.vpipeline.api.modules.AttachedPipeline;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.function.Function;

public class MySQLStorage extends SQLStorage {
    private final HikariConfig hikariConfig;
    private final AttachedPipeline attachedPipeline;
    private HikariDataSource hikariDataSource;

    public MySQLStorage(HikariConfig hikariConfig) {
        this.hikariConfig = hikariConfig;
        this.attachedPipeline = new AttachedPipeline(GsonBuilder::create);
        NetworkLogger.info("MySQL Global Storage connected");
    }

    @Override
    public void connect() {
        this.hikariDataSource = new HikariDataSource(hikariConfig);
    }

    @Override
    public void disconnect() {
        this.hikariDataSource.close();
    }

    @Override
    public @NotNull Connection connection() {
        try {
            return this.hikariDataSource.getConnection();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to retrieve connection from pool", e);
        }
    }

    @Override
    public int executeUpdate(@NotNull String query, @NotNull Object... objects) {
        try (Connection con = this.connection(); PreparedStatement statement = con.prepareStatement(query)) {
            // write all parameters
            for (int i = 0; i < objects.length; i++) {
                statement.setString(i + 1, Objects.toString(objects[i]));
            }

            // execute the statement
            return statement.executeUpdate();
        } catch (SQLException exception) {
            exception.printStackTrace();
            return -1;
        }
    }

    @Override
    public <T> T executeQuery(@NotNull String query, @NotNull Function<ResultSet, T> callback, @Nullable T def, @NotNull Object... objects) {
        try (Connection con = this.connection(); PreparedStatement statement = con.prepareStatement(query)) {
            // write all parameters
            for (int i = 0; i < objects.length; i++) {
                statement.setString(i + 1, Objects.toString(objects[i]));
            }

            // execute the statement, apply to the result handler
            try (var resultSet = statement.executeQuery()) {
                return callback.apply(resultSet);
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

        return def;
    }

    @Override
    public AttachedPipeline getAttachedPipeline() {
        return attachedPipeline;
    }

    @Override
    public void shutdown() {
        this.hikariDataSource.close();
    }
}
