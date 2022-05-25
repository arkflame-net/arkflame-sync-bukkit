package com.arkflame.arkflamesync;

import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.Jedis;

import java.util.logging.Logger;

import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.plugin.java.JavaPlugin;

public class ArkFlameSync extends JavaPlugin {
    private Jedis client;
    private JedisPubSub pubsub;
    private BukkitTask task;

    private void giveRank(String player) {
        Server server = getServer();
        String rank = "youtube";
        String duration = "7d";

        server.dispatchCommand(
                server.getConsoleSender(),
                "lp user {player} parent removetemp {rank}"
                        .replace("{player}", player)
                        .replace("{rank}", rank));
        server.dispatchCommand(
                server.getConsoleSender(),
                "lp user {player} parent addtemp {rank} {duration}"
                        .replace("{player}", player)
                        .replace("{rank}", rank)
                        .replace("{duration}", duration));
    }

    public void onEnable() {
        Plugin plugin = this;
        Logger logger = getLogger();
        Server server = getServer();
        BukkitScheduler scheduler = server.getScheduler();

        this.saveDefaultConfig();

        this.client = new Jedis(this.getConfig().getString("redis_uri"));
        this.pubsub = new JedisPubSub() {
            @Override
            public void onMessage(String channel, String nick) {
                scheduler.runTask(plugin, () -> {
                    if (nick.matches("^[a-zA-Z0-9_]{2,16}$")) {
                        giveRank(nick);
                    } else {
                        logger.severe("Tried to process an invalid nickname: " + nick);
                    }
                });
            }
        };

        this.task = scheduler.runTaskAsynchronously(plugin, () -> {
            client.subscribe(pubsub, "arkflame-sync-yt");
            client.close();
        });
    }

    public void onDisable() {
        if (pubsub != null) {
            pubsub.unsubscribe("arkflame-sync-yt");
        }

        if (task != null) {
            task.cancel();
        }
    }
}
