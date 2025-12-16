package dev.jalikdev.lowCoreQuests.gui;

import dev.jalikdev.lowCore.LowCore;
import dev.jalikdev.lowCoreQuests.service.QuestService;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class QuestGuiListener implements Listener {

    private final LowCore core;
    private final QuestService service;

    public QuestGuiListener(LowCore core, QuestService service) {
        this.core = core;
        this.service = service;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getCurrentItem() == null) return;

        String title = ChatColor.stripColor(e.getView().getTitle());
        boolean questMenu = title != null && title.equalsIgnoreCase(QuestMenu.TITLE);
        boolean rewardMenu = title != null && title.equalsIgnoreCase(RewardMenu.TITLE);

        if (!questMenu && !rewardMenu) return;

        e.setCancelled(true);

        ItemMeta meta = e.getCurrentItem().getItemMeta();
        if (meta == null) return;

        if (questMenu && e.getRawSlot() == 49) {
            player.closeInventory();
            return;
        }

        if (rewardMenu) {
            Integer idx = meta.getPersistentDataContainer().get(RewardMenu.KEY_REWARD_INDEX, PersistentDataType.INTEGER);
            String qid = meta.getPersistentDataContainer().get(RewardMenu.KEY_QUEST_ID, PersistentDataType.STRING);
            if (idx == null || qid == null) return;

            service.claimChoice(player, qid, idx);
            player.closeInventory();
            return;
        }

        String action = meta.getPersistentDataContainer().get(QuestMenu.KEY_ACTION, PersistentDataType.STRING);
        if (action == null) return;

        switch (action) {
            case "story" -> {
                var def = service.startNextStory(player);
                if (def == null) player.sendMessage(core.getPrefix() + "No story quest available or you already have a quest.");
                else player.sendMessage(core.getPrefix() + "Story quest started: " + ChatColor.stripColor(def.name()));
                player.openInventory(QuestMenu.build(core, service, player));
            }
            case "random" -> {
                var def = service.startRandom(player);
                if (def == null) player.sendMessage(core.getPrefix() + "No random quest available or you already have a quest.");
                else player.sendMessage(core.getPrefix() + "Random quest started: " + ChatColor.stripColor(def.name()));
                player.openInventory(QuestMenu.build(core, service, player));
            }
            case "cancel" -> {
                boolean ok = service.cancel(player);
                player.sendMessage(core.getPrefix() + (ok ? "Quest cancelled." : "No active quest."));
                player.openInventory(QuestMenu.build(core, service, player));
            }
            case "turnin" -> {
                int added = service.turnInItems(player);
                player.sendMessage(core.getPrefix() + (added > 0 ? ("Turned in " + added + " items.") : "No items to turn in."));
                player.openInventory(QuestMenu.build(core, service, player));
            }
            case "complete" -> {
                if (!service.canComplete(player)) {
                    player.sendMessage(core.getPrefix() + "Not complete yet.");
                    return;
                }
                service.completeOrOpenRewards(player);

                String now = ChatColor.stripColor(player.getOpenInventory().getTitle());
                if (now != null && now.equalsIgnoreCase(RewardMenu.TITLE)) return;

                player.openInventory(QuestMenu.build(core, service, player));
            }
        }
    }
}
