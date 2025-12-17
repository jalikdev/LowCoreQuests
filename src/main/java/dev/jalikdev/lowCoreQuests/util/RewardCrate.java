package dev.jalikdev.lowCoreQuests.util;

import dev.jalikdev.lowCoreQuests.reward.Reward;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.nio.charset.StandardCharsets;
import java.util.*;

public final class RewardCrate {

    public static final NamespacedKey KEY_CRATE = new NamespacedKey("lowcorequests", "reward_crate");
    public static final NamespacedKey KEY_DATA = new NamespacedKey("lowcorequests", "reward_data");
    public static final NamespacedKey KEY_TITLE = new NamespacedKey("lowcorequests", "reward_title");

    private RewardCrate() {}

    public static ItemStack create(String title, List<Reward> rewards) {
        ItemStack it = new ItemStack(Material.CHEST_MINECART);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(Text.c(title));
        meta.getPersistentDataContainer().set(KEY_CRATE, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(KEY_TITLE, PersistentDataType.STRING, title == null ? "" : title);

        List<Map<String, Object>> list = new ArrayList<>();
        for (Reward r : rewards) {
            Map<String, Object> m = r.toMap();
            if (m != null && !m.isEmpty()) list.add(m);
        }

        YamlConfiguration y = new YamlConfiguration();
        y.set("rewards", list);
        String s = y.saveToString();
        String b64 = Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
        meta.getPersistentDataContainer().set(KEY_DATA, PersistentDataType.STRING, b64);

        meta.setLore(List.of(
                Text.c("&7Right click to claim."),
                Text.c("&7Nothing will vanish if inventory is full.")
        ));
        it.setItemMeta(meta);
        return it;
    }

    public static boolean isCrate(ItemStack it) {
        if (it == null || it.getType().isAir()) return false;
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return false;
        Byte b = meta.getPersistentDataContainer().get(KEY_CRATE, PersistentDataType.BYTE);
        return b != null && b == (byte) 1;
    }

    public static List<Reward> readRewards(ItemStack it) {
        if (!isCrate(it)) return List.of();
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return List.of();

        String b64 = meta.getPersistentDataContainer().get(KEY_DATA, PersistentDataType.STRING);
        if (b64 == null || b64.isBlank()) return List.of();

        String s;
        try {
            s = new String(Base64.getDecoder().decode(b64), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return List.of();
        }

        YamlConfiguration y = new YamlConfiguration();
        try {
            y.loadFromString(s);
        } catch (Exception e) {
            return List.of();
        }

        List<?> raw = y.getList("rewards");
        if (raw == null) return List.of();

        List<Reward> out = new ArrayList<>();
        for (Object o : raw) {
            if (o instanceof Map<?, ?> m) {
                Reward r = Reward.fromMap(m);
                if (r != null) out.add(r);
            }
        }
        return out;
    }

    public static void giveCrateOrDrop(Player player, ItemStack crate) {
        Map<Integer, ItemStack> left = player.getInventory().addItem(crate);
        if (!left.isEmpty()) {
            for (ItemStack x : left.values()) player.getWorld().dropItemNaturally(player.getLocation(), x);
        }
    }

    public static void giveItemsOrDrop(Player player, ItemStack... items) {
        for (ItemStack it : items) {
            if (it == null || it.getType().isAir()) continue;
            Map<Integer, ItemStack> left = player.getInventory().addItem(it);
            if (!left.isEmpty()) {
                for (ItemStack x : left.values()) player.getWorld().dropItemNaturally(player.getLocation(), x);
            }
        }
    }

    public static void consumeOneInHand(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) return;
        if (!isCrate(hand)) return;

        int amt = hand.getAmount();
        if (amt <= 1) player.getInventory().setItemInMainHand(null);
        else {
            hand.setAmount(amt - 1);
            player.getInventory().setItemInMainHand(hand);
        }
    }
}
