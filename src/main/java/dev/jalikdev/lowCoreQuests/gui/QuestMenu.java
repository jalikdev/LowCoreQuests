package dev.jalikdev.lowCoreQuests.gui;

import dev.jalikdev.lowCore.LowCore;
import dev.jalikdev.lowCoreQuests.model.PlayerQuestState;
import dev.jalikdev.lowCoreQuests.model.QuestDefinition;
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

public class QuestMenu {

    public static final String TITLE = "LowCore Quests";

    public static final NamespacedKey KEY_ACTION = new NamespacedKey("lowcorequests", "lcq_action");
    public static final NamespacedKey KEY_QUEST = new NamespacedKey("lowcorequests", "lcq_quest");

    public static Inventory build(LowCore core, QuestService service, Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, Text.c("&a" + TITLE));

        fill(inv);

        inv.setItem(4, item(Material.BOOK, "&fQuest Menu", List.of("&7One active quest at a time")));
        inv.setItem(49, item(Material.BARRIER, "&cClose", List.of("&7Click to close")));

        PlayerQuestState st = service.getActive(player.getUniqueId());

        if (st == null) {
            inv.setItem(22, item(Material.PAPER, "&fNo active quest", List.of("&7You get a random quest")));
            inv.setItem(31, action(Material.LIME_CONCRETE, "&aGet Random Quest", "random", null));
            return inv;
        }

        QuestDefinition q = service.getQuest(st.questId());
        if (q == null) {
            inv.setItem(22, item(Material.BARRIER, "&cActive quest missing", List.of("&7Cancel it")));
            inv.setItem(31, action(Material.BARRIER, "&cCancel", "cancel", null));
            return inv;
        }

        inv.setItem(22, activeQuestItem(q, st));

        inv.setItem(29, action(Material.BARRIER, "&cCancel", "cancel", null));
        inv.setItem(31, action(Material.LIME_CONCRETE, "&aComplete", "complete", null));

        if (q.type() == QuestType.ITEM) {
            inv.setItem(33, action(Material.CHEST, "&eTurn in items", "turnin", null));
        } else {
            inv.setItem(33, item(Material.GRAY_DYE, "&7Turn in items", List.of("&7Not an ITEM quest")));
        }

        return inv;
    }

    private static ItemStack activeQuestItem(QuestDefinition q, PlayerQuestState st) {
        List<String> lore = new ArrayList<>();

        lore.add(Text.c("&7Difficulty: &f" + q.difficulty()));
        lore.add(Text.c("&7Type: &f" + q.type()));
        lore.add(Text.c("&7Goal: &f" + goalText(q)));
        lore.add(Text.c("&7Progress: &f" + st.progress() + "/" + q.objective().required()));
        lore.add(Text.c(st.progress() >= q.objective().required() ? "&aReady" : "&eIn progress"));

        if (q.description() != null && !q.description().isEmpty()) {
            lore.add(" ");
            for (String line : q.description()) {
                lore.add(Text.c(line));
            }
        }

        return item(Material.BOOK, q.name(), lore);
    }

    private static String goalText(QuestDefinition q) {
        String custom = q.objective().displayName();
        if (custom != null && !custom.isBlank()) {
            if (q.type() == QuestType.ITEM) return q.objective().required() + "x " + Text.c(custom);
            if (q.type() == QuestType.KILL_MOB) return q.objective().required() + "x " + Text.c(custom);
            if (q.type() == QuestType.BIOME) return "Enter " + Text.c(custom);
        }

        if (q.type() == QuestType.ITEM) return q.objective().required() + "x " + q.objective().material().name();
        if (q.type() == QuestType.BIOME) return "Enter " + q.objective().biomeKey().getKey();
        if (q.type() == QuestType.KILL_MOB) return q.objective().required() + "x " + q.objective().entityType().name();
        return "Unknown";
    }

    private static ItemStack action(Material mat, String name, String action, String questId) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(Text.c(name));
        meta.getPersistentDataContainer().set(KEY_ACTION, PersistentDataType.STRING, action);
        if (questId != null) meta.getPersistentDataContainer().set(KEY_QUEST, PersistentDataType.STRING, questId);
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
