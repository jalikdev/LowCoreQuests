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

        NamespacedKey structureKey,
        int searchRadius,
        int nearBlocks,

        String displayName
) {

    public static QuestObjectiveDefinition collect(Material material, int required, String displayName) {
        return new QuestObjectiveDefinition(QuestType.COLLECT, required, material, null, null, null, 0, 0, displayName);
    }

    public static QuestObjectiveDefinition deliver(Material material, int required, String displayName) {
        return new QuestObjectiveDefinition(QuestType.DELIVER, required, material, null, null, null, 0, 0, displayName);
    }

    public static QuestObjectiveDefinition breakBlock(Material material, int required, String displayName) {
        return new QuestObjectiveDefinition(QuestType.BREAK, required, material, null, null, null, 0, 0, displayName);
    }

    public static QuestObjectiveDefinition biome(NamespacedKey biomeKey, int required, String displayName) {
        return new QuestObjectiveDefinition(QuestType.BIOME, required, null, biomeKey, null, null, 0, 0, displayName);
    }

    public static QuestObjectiveDefinition kill(EntityType entityType, int required, String displayName) {
        return new QuestObjectiveDefinition(QuestType.KILL_MOB, required, null, null, entityType, null, 0, 0, displayName);
    }

    public static QuestObjectiveDefinition structure(NamespacedKey structureKey, int required, int searchRadius, int nearBlocks, String displayName) {
        return new QuestObjectiveDefinition(QuestType.STRUCTURE, required, null, null, null, structureKey, searchRadius, nearBlocks, displayName);
    }
}
