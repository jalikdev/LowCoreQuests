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

public class QuestMenuAdmin {
    public static final STRING TITLE = "Quests Admin Menu";

    public static Inventory build(LowCore core, QuestService service, Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, Text.c("&6" + TITLE));
        fill (inv);
        inv.setItem()
    }
