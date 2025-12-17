package dev.jalikdev.lowCoreQuests.gui;

import dev.jalikdev.lowCoreQuests.model.QuestDefinition;
import dev.jalikdev.lowCoreQuests.reward.Reward;
import dev.jalikdev.lowCoreQuests.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class RewardMenu {

    public static final String TITLE = "Choose Reward";
    public static final NamespacedKey KEY_REWARD_INDEX = new NamespacedKey("lowcorequests", "reward_index");
    public static final NamespacedKey KEY_QUEST_ID = new NamespacedKey("lowcorequests", "quest_id");

    private RewardMenu() {}

    public static Inventory build(QuestDefinition quest) {
        int size = 27;
        Inventory inv = Bukkit.createInventory(null, size, Text.c("&a" + TITLE));

        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta gm = glass.getItemMeta();
        gm.setDisplayName(" ");
        glass.setItemMeta(gm);
        for (int i = 0; i < size; i++) inv.setItem(i, glass);

        inv.setItem(13, item(Material.BOOK, quest.name(), loreQuest(quest)));

        List<Reward> rewards = quest.rewards().options();
        int n = rewards.size();

        List<Integer> slots = centeredSlots(n);

        for (int i = 0; i < n; i++) {
            Reward r = rewards.get(i);
            int slot = slots.get(i);

            ItemStack it = new ItemStack(Material.CHEST);
            ItemMeta meta = it.getItemMeta();
            meta.setDisplayName(Text.c("&fReward " + (i + 1)));
            meta.setLore(List.of(Text.c("&7" + r.display()), Text.c("&aClick to get a Reward Crate")));
            meta.getPersistentDataContainer().set(KEY_REWARD_INDEX, PersistentDataType.INTEGER, i);
            meta.getPersistentDataContainer().set(KEY_QUEST_ID, PersistentDataType.STRING, quest.id());
            it.setItemMeta(meta);

            inv.setItem(slot, it);
        }

        inv.setItem(26, closeItem());

        return inv;
    }

    private static List<String> loreQuest(QuestDefinition quest) {
        List<String> lore = new ArrayList<>();
        lore.add(Text.c("&7Pick exactly one option."));
        lore.add(Text.c("&7You will receive a Reward Crate."));
        lore.add(Text.c("&7Crate can be opened anytime."));
        return lore;
    }

    private static ItemStack closeItem() {
        ItemStack it = new ItemStack(Material.BARRIER);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(Text.c("&cClose"));
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

    private static List<Integer> centeredSlots(int n) {
        List<Integer> out = new ArrayList<>();
        if (n <= 0) return out;

        int rowStart = 9;
        int rowEnd = 17;

        List<Integer> row = new ArrayList<>();
        for (int s = rowStart; s <= rowEnd; s++) row.add(s);

        if (n >= 9) {
            for (int i = 0; i < Math.min(9, n); i++) out.add(row.get(i));
            return out;
        }

        int start = (9 - n) / 2;
        for (int i = 0; i < n; i++) out.add(row.get(start + i));
        return out;
    }
}
