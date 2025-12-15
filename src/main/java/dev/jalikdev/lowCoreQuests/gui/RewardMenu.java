package dev.jalikdev.lowCoreQuests.gui;

import dev.jalikdev.lowCore.LowCore;
import dev.jalikdev.lowCoreQuests.model.QuestDefinition;
import dev.jalikdev.lowCoreQuests.reward.Reward;
import dev.jalikdev.lowCoreQuests.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class RewardMenu {

    public static final String TITLE = "Select Reward";

    public static final NamespacedKey KEY_REWARD_INDEX = new NamespacedKey(LowCore.getInstance(), "lcq_reward_idx");
    public static final NamespacedKey KEY_QUEST_ID = new NamespacedKey(LowCore.getInstance(), "lcq_reward_qid");

    public static Inventory build(LowCore core, QuestDefinition def) {
        Inventory inv = Bukkit.createInventory(null, 27, Text.c("&a" + TITLE));

        fill(inv);

        List<Reward> opts = def.rewards().options();
        int slot = 10;

        for (int i = 0; i < opts.size() && slot <= 16; i++) {
            Reward r = opts.get(i);

            ItemStack it = icon(r);
            ItemMeta meta = it.getItemMeta();
            meta.setDisplayName(Text.c("&f" + r.display()));
            meta.setLore(List.of(Text.c("&7Pick exactly one"), Text.c("&aClick to claim")));
            meta.getPersistentDataContainer().set(KEY_REWARD_INDEX, PersistentDataType.INTEGER, i);
            meta.getPersistentDataContainer().set(KEY_QUEST_ID, PersistentDataType.STRING, def.id());
            it.setItemMeta(meta);

            inv.setItem(slot++, it);
        }

        return inv;
    }

    private static ItemStack icon(Reward r) {
        String d = r.display().toUpperCase();
        if (d.contains("XP")) return new ItemStack(Material.EXPERIENCE_BOTTLE);
        if (d.contains("COMMAND")) return new ItemStack(Material.COMMAND_BLOCK);
        return new ItemStack(Material.CHEST);
    }

    private static void fill(Inventory inv) {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta m = glass.getItemMeta();
        m.setDisplayName(" ");
        glass.setItemMeta(m);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, glass);
    }
}
