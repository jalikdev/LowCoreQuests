package dev.jalikdev.lowCoreQuests.model;

public record QuestDefinition(
        String id,
        boolean enabled,
        String name,
        Difficulty difficulty,
        QuestType type,
        QuestObjective objective,
        dev.jalikdev.lowCoreQuests.reward.RewardBundle rewards
) { }
