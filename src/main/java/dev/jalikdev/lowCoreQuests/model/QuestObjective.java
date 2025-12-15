package dev.jalikdev.lowCoreQuests.model;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;

public record QuestObjective(
        Material material,
        NamespacedKey biomeKey,
        EntityType entityType,
        int required,
        String displayName
) {
    public static QuestObjective item(Material mat, int required, String displayName) {
        return new QuestObjective(mat, null, null, required, displayName);
    }

    public static QuestObjective biome(NamespacedKey key, int required, String displayName) {
        return new QuestObjective(null, key, null, required, displayName);
    }

    public static QuestObjective kill(EntityType type, int required, String displayName) {
        return new QuestObjective(null, null, type, required, displayName);
    }
}
