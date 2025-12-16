package dev.jalikdev.lowCoreQuests.gui;

import dev.jalikdev.lowCore.LowCore;
import dev.jalikdev.lowCoreQuests.model.PlayerQuestState;
import dev.jalikdev.lowCoreQuests.model.QuestDefinition;
import dev.jalikdev.lowCoreQuests.model.QuestObjectiveDefinition;
import dev.jalikdev.lowCoreQuests.model.QuestType;
import dev.jalikdev.lowCoreQuests.service.QuestService;
import dev.jalikdev.lowCoreQuests.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class QuestMenu {

    public static final String TITLE = "LowCore Quests";
    public static final NamespacedKey KEY_ACTION = new NamespacedKey("lowcorequests", "lcq_action");

    public static Inventory build(LowCore core, QuestService service, Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, Text.c("&a" + TITLE));
        fill(inv);

        inv.setItem(3, item(Material.BOOK, "&fQuest Menu", List.of("&7Story + Random quests")));
        inv.setItem(49, item(Material.BARRIER, "&cClose", List.of("&7Click to close")));

        var stats = service.getStats(player.getUniqueId());
        inv.setItem(5, item(Material.CLOCK, "&fQuest Stats", List.of(
                Text.c("&7Completed: &f" + stats.total()),
                Text.c("&7Story: &f" + stats.story()),
                Text.c("&7Random: &f" + stats.random())
        )));

        PlayerQuestState st = service.getActive(player.getUniqueId());

        if (st == null) {
            QuestDefinition nextStory = service.getNextStory(player.getUniqueId());

            List<String> info = new ArrayList<>();
            if (nextStory == null) {
                info.add(Text.c("&7No more story quests."));
            } else {
                info.add(Text.c("&7Next Story:"));
                info.add(Text.c("&f" + nextStory.name()));
                info.add(Text.c("&7Difficulty: &f" + nextStory.difficulty()));
            }

            inv.setItem(22, item(Material.PAPER, "&fNo active quest", info));

            inv.setItem(20, action(Material.WRITABLE_BOOK, "&aStory Quest", "story"));
            inv.setItem(24, action(Material.COMPASS, "&eRandom Quest", "random"));

            return inv;
        }

        service.syncCollectProgress(player);

        QuestDefinition q = service.getQuestById(st.questId());
        if (q == null) {
            inv.setItem(22, item(Material.BARRIER, "&cActive quest missing", List.of("&7Cancel it")));
            inv.setItem(31, action(Material.BARRIER, "&cCancel", "cancel"));
            return inv;
        }

        inv.setItem(22, activeQuestItem(service, player.getUniqueId(), q));

        inv.setItem(29, action(Material.BARRIER, "&cCancel", "cancel"));
        inv.setItem(31, action(Material.LIME_CONCRETE, "&aComplete", "complete"));

        boolean hasDeliver = q.objectives().stream().anyMatch(o -> o.type() == QuestType.DELIVER);
        if (hasDeliver) {
            inv.setItem(33, action(Material.CHEST, "&eTurn in items", "turnin"));
        } else {
            inv.setItem(33, item(Material.GRAY_DYE, "&7Turn in items", List.of("&7This quest has no DELIVER objectives")));
        }

        return inv;
    }

    private static ItemStack activeQuestItem(QuestService service, UUID uuid, QuestDefinition q) {
        List<String> lore = new ArrayList<>();
        lore.add(Text.c("&7Difficulty: &f" + q.difficulty()));
        lore.add(" ");

        Map<Integer, Integer> prog = service.getProgressMap(uuid);

        for (int i = 0; i < q.objectives().size(); i++) {
            QuestObjectiveDefinition obj = q.objectives().get(i);
            int cur = prog.getOrDefault(i, 0);
            lore.add(Text.c("&7- &f" + objectiveLine(obj) + " &7(&f" + cur + "&7/&f" + obj.required() + "&7)"));
        }

        lore.add(" ");
        Player p = Bukkit.getPlayer(uuid);
        lore.add(Text.c("&7Status: " + ((p != null && service.canComplete(p)) ? "&aReady" : "&eIn progress")));

        if (q.description() != null && !q.description().isEmpty()) {
            lore.add(" ");
            for (String line : q.description()) lore.add(Text.c(line));
        }

        return item(Material.BOOK, q.name(), lore);
    }

    private static String objectiveLine(QuestObjectiveDefinition obj) {
        String disp = obj.displayName();
        String base = (disp != null && !disp.isBlank()) ? Text.c(disp) : fallbackName(obj);

        return switch (obj.type()) {
            case BIOME -> "Enter " + base;
            case KILL_MOB -> "Kill " + base;
            case DELIVER -> "Deliver " + base;
            case COLLECT -> "Collect " + base;
            case STRUCTURE -> "Find " + base;
        };
    }

    private static String fallbackName(QuestObjectiveDefinition obj) {
        if (obj.type() == QuestType.BIOME) return obj.biomeKey().getKey();
        if (obj.type() == QuestType.KILL_MOB) return obj.entityType().name();
        if (obj.type() == QuestType.STRUCTURE) return obj.structureKey().getKey();
        return obj.material().name();
    }

    private static ItemStack action(Material mat, String name, String action) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(Text.c(name));
        meta.getPersistentDataContainer().set(KEY_ACTION, PersistentDataType.STRING, action);
        it.setItemMeta(meta);
        return it;
    }

    private static ItemStack item(Material mat, String name, List<String> lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(Text.c(name));
        meta.setLore(lore);
        it.setItemMeta(meta);
        return it;
    }

    private static void fill(Inventory inv) {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta m = glass.getItemMeta();
        m.setDisplayName(" ");
        glass.setItemMeta(m);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, glass);
    }
}
