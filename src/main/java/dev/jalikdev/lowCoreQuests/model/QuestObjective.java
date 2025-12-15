package dev.jalikdev.lowCoreQuests.model;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;

public record QuestObjective(Material material, NamespacedKey biomeKey, EntityType entityType, int required) {

    public static QuestObjective item(Material mat, int required) {
        return new QuestObjective(mat, null, null, required);
    }

    public static QuestObjective biome(NamespacedKey key, int required) {
        return new QuestObjective(null, key, null, required);
    }

    public static QuestObjective kill(EntityType type, int required) {
        return new QuestObjective(null, null, type, required);
    }
}
