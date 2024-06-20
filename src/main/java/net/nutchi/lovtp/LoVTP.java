package net.nutchi.lovtp;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class LoVTP extends JavaPlugin implements Listener {
    private String spawnWorldName;
    private final Map<UUID, Location> playerLastLocation = new HashMap<>();
    private Location defaultSpawnLocation;

    private Storage storage;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        spawnWorldName = getConfig().getString("spawn-world");
        if (spawnWorldName == null) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        String locationString = getConfig().getString("default-spawn-location");
        if (locationString == null) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (convertStringToLocation(locationString).isPresent()) {
            defaultSpawnLocation = convertStringToLocation(locationString).get();
        } else {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        String mysqlHost = getConfig().getString("mysql.host");
        int mysqlPort = getConfig().getInt("mysql.port");
        String mysqlDatabase = getConfig().getString("mysql.database");
        String mysqlUsername = getConfig().getString("mysql.username");
        String mysqlPassword = getConfig().getString("mysql.password");
        String mysqlTablePrefix = getConfig().getString("mysql.table-prefix");

        if (mysqlHost == null || mysqlDatabase == null || mysqlUsername == null || mysqlPassword == null) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        storage = new Storage(this, mysqlHost, mysqlPort, mysqlDatabase, mysqlUsername, mysqlPassword, mysqlTablePrefix);

        if (!storage.init()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, String label, String[] args) {
        switch (label) {
            case "lovtp":
                if (args.length == 1) {
                    Player player = getServer().getPlayer(args[0]);
                    if (player != null) {
                        if (playerLastLocation.containsKey(player.getUniqueId())) {
                            player.teleport(playerLastLocation.get(player.getUniqueId()));
                        } else {
                            player.teleport(defaultSpawnLocation);
                        }
                        return true;
                    }
                }
                break;
            case "lovtpreload":
                reloadConfig();
                spawnWorldName = getConfig().getString("spawn-world");
                return true;
            case "lovtpsetspawn":
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    convertLocationToString(player.getLocation()).ifPresent(l -> {
                        getConfig().set("default-spawn-location", l);
                        saveConfig();
                        defaultSpawnLocation = player.getLocation();
                    });

                    return true;
                }
                break;
        }

        return false;
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        storage.loadLastLocation(event.getPlayer().getUniqueId()).ifPresent(l -> playerLastLocation.put(event.getPlayer().getUniqueId(), l));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        World world = event.getPlayer().getLocation().getWorld();
        if (world != null && !world.getName().equals(spawnWorldName)) {
            storage.saveLastLocation(event.getPlayer().getUniqueId(), event.getPlayer().getLocation());
        }

        playerLastLocation.remove(event.getPlayer().getUniqueId());
    }

    public Optional<Location> convertStringToLocation(String locationString) {
        String[] location = locationString.split(",");
        if (location.length != 6) {
            return Optional.empty();
        }
        World world = getServer().getWorld(location[0]);
        if (world == null) {
            return Optional.empty();
        }
        return Optional.of(new Location(world, Double.parseDouble(location[1]), Double.parseDouble(location[2]), Double.parseDouble(location[3]), Float.parseFloat(location[4]), Float.parseFloat(location[5])));
    }

    public Optional<String> convertLocationToString(Location location) {
        if (location.getWorld() != null) {
            return Optional.of(location.getWorld().getName() + "," + location.getX() + "," + location.getY() + "," + location.getZ() + "," + location.getYaw() + "," + location.getPitch());
        } else {
            return Optional.empty();
        }
    }
}
