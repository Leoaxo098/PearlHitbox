package com.pearlhitbox;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class PearlHitboxPlugin extends JavaPlugin {

    private HitboxConfig hitboxConfig;
    private PearlHitboxTask task;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.hitboxConfig = new HitboxConfig(this);

        this.task = new PearlHitboxTask(this, hitboxConfig);
        task.start();

        getServer().getPluginManager().registerEvents(new PearlHitboxListener(task, hitboxConfig), this);

        getLogger().info("PearlHitbox enabled - radius=" + hitboxConfig.getRadius()
                + " onlyWindCharge=" + hitboxConfig.isOnlyWindCharge());
    }

    @Override
    public void onDisable() {
        if (task != null) {
            task.stop();
        }
    }

    public HitboxConfig getHitboxConfig() {
        return hitboxConfig;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                              @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("pearlhitbox.admin")) {
            sender.sendMessage("§cYou don't have permission to do that.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§eUsage: /" + label + " <reload|info>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                reloadConfig();
                hitboxConfig.reload();
                task.restart();
                sender.sendMessage("§aPearlHitbox config reloaded.");
            }
            case "info" -> {
                sender.sendMessage("§6PearlHitbox §7- radius=§f" + hitboxConfig.getRadius()
                        + " §7interval=§f" + hitboxConfig.getCheckIntervalTicks() + "t"
                        + " §7mode=§f" + hitboxConfig.getMode()
                        + " §7onlyWindCharge=§f" + hitboxConfig.isOnlyWindCharge());
            }
            default -> sender.sendMessage("§eUsage: /" + label + " <reload|info>");
        }
        return true;
    }
}
