package net.nutchi.lovtp;

import lombok.RequiredArgsConstructor;
import org.bukkit.Location;

import java.sql.*;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class Storage {
    private final LoVTP plugin;

    private final String mysqlHost;
    private final int mysqlPort;
    private final String mysqlDatabase;
    private final String mysqlUsername;
    private final String mysqlPassword;
    private final String mysqlTablePrefix;

    public boolean init() {
        try {
            Connection connection = getConnection();

            String sql = String.format("CREATE TABLE IF NOT EXISTS %splayer_last_locations (" +
                    "player_uuid varchar(36) NOT NULL," +
                    "location text NOT NULL," +
                    "UNIQUE(player_uuid)" +
                    ")", mysqlTablePrefix);
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.executeUpdate();

            statement.close();
            connection.close();

            return true;
        } catch (SQLException e) {
            e.printStackTrace();

            return false;
        }
    }

    public Optional<Location> loadLastLocation(UUID playerUuid) {
        try {
            Connection connection = getConnection();

            String sql = String.format("SELECT location FROM %splayer_last_locations WHERE player_uuid = ?", mysqlTablePrefix);
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, playerUuid.toString());
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                String locationString = resultSet.getString("location");

                statement.close();
                connection.close();

                return plugin.convertStringToLocation(locationString);
            } else {
                statement.close();
                connection.close();

                return Optional.empty();
            }
        } catch (SQLException e) {
            e.printStackTrace();

            return Optional.empty();
        }
    }

    public void saveLastLocation(UUID playerUuid, Location location) {
        try {
            Connection connection = getConnection();

            String sql = String.format("REPLACE INTO %splayer_last_locations (player_uuid, location) VALUES (?, ?)", mysqlTablePrefix);
            PreparedStatement statement = connection.prepareStatement(sql);
            if (plugin.convertLocationToString(location).isPresent()) {
                statement.setString(1, playerUuid.toString());
                statement.setString(2, plugin.convertLocationToString(location).get());
                statement.executeUpdate();
            }

            statement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Connection getConnection() throws SQLException {
        String url = "jdbc:mysql://" + mysqlHost + ":" + mysqlPort + "/" + mysqlDatabase;
        return DriverManager.getConnection(url, mysqlUsername, mysqlPassword);
    }
}
