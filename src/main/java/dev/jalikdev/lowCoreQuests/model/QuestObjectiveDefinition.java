package dev.jalikdev.lowCoreQuests.model;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;

public record QuestObjectiveDefinition(
        QuestType type,
        int required,
        Material material,
        NamespacedKey biomeKey,
        EntityType entityType,
        String displayName
) {
    public static QuestObjectiveDefinition item(QuestType type, Material material, int required, String displayName) {
        return new QuestObjectiveDefinition(type, required, material, null, null, displayName);
    }

    public static QuestObjectiveDefinition biome(NamespacedKey biomeKey, int required, String displayName) {
        return new QuestObjectiveDefinition(QuestType.BIOME, required, null, biomeKey, null, displayName);
    }

    public static QuestObjectiveDefinition kill(EntityType entityType, int required, String displayName) {
        return new QuestObjectiveDefinition(QuestType.KILL_MOB, required, null, null, entityType, displayName);
    }
}
