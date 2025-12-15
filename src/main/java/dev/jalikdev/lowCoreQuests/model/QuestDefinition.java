package dev.jalikdev.lowCoreQuests.model;

import dev.jalikdev.lowCoreQuests.reward.RewardBundle;

import java.util.List;

public record QuestDefinition(
        String id,
        boolean enabled,
        String name,
        List<String> description,
        Difficulty difficulty,
        QuestType type,
        QuestObjective objective,
        RewardBundle rewards
) { }
