package dev.jalikdev.lowCoreQuests.command;

import dev.jalikdev.lowCore.LowCore;
import dev.jalikdev.lowCoreQuests.gui.QuestMenu;
import dev.jalikdev.lowCoreQuests.service.QuestService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class QuestsCommand implements CommandExecutor {

    private final LowCore core;
    private final QuestService service;

    public QuestsCommand(LowCore core, QuestService service) {
        this.core = core;
        this.service = service;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            LowCore.sendConfigMessage(sender, "player-only");
            return true;
        }

        if (args.length == 0) {
            player.openInventory(QuestMenu.build(core, service, player));
            return true;
        }

        if (args.equals("admin")) {
            if (!player.hasPermission("lowcore.quests")) {
                LowCore.sendConfigMessage(player, "no-permission");
                return true;
            }
            player.openInventory(QuestMenuAdmin.build(core, service, player));
        }
    }
}
