package dev.jalikdev.lowCoreQuests.config;

import dev.jalikdev.lowCore.LowCore;
import dev.jalikdev.lowCoreQuests.LowCoreQuests;
import dev.jalikdev.lowCoreQuests.model.Difficulty;
import dev.jalikdev.lowCoreQuests.model.QuestDefinition;
import dev.jalikdev.lowCoreQuests.model.QuestObjective;
import dev.jalikdev.lowCoreQuests.model.QuestType;
import dev.jalikdev.lowCoreQuests.reward.Reward;
import dev.jalikdev.lowCoreQuests.reward.RewardBundle;
import dev.jalikdev.lowCoreQuests.reward.RewardMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;

public class QuestConfig {

    private final LowCoreQuests addon;
    private final LowCore core;

    private Map<String, QuestDefinition> quests = new LinkedHashMap<>();

    public QuestConfig(LowCoreQuests addon, LowCore core) {
        this.addon = addon;
        this.core = core;
    }

    public File questsFile() {
        return new File(core.getDataFolder(), "quests.yml");
    }

    public void ensureQuestsFileInLowCoreFolder() {
        if (!core.getDataFolder().exists()) core.getDataFolder().mkdirs();

        File file = questsFile();
        if (file.exists()) return;

        try (InputStream in = addon.getResource("quests.yml")) {
            if (in == null) {
                addon.getLogger().severe("Default quests.yml missing in addon resources.");
                return;
            }
            Files.copy(in, file.toPath());
            addon.getLogger().info("Created quests.yml in LowCore folder: " + file.getPath());
        } catch (IOException e) {
            addon.getLogger().severe("Failed to create quests.yml in LowCore folder.");
            e.printStackTrace();
        }
    }

    public void reload() {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(questsFile());
        ConfigurationSection root = cfg.getConfigurationSection("quests");

        Map<String, QuestDefinition> map = new LinkedHashMap<>();

        if (root != null) {
            for (String id : root.getKeys(false)) {
                ConfigurationSection q = root.getConfigurationSection(id);
                if (q == null) continue;

                boolean enabled = q.getBoolean("enabled", true);
                String name = q.getString("name", id);

                List<String> description = q.getStringList("description");

                Difficulty difficulty;
                try {
                    difficulty = Difficulty.valueOf(q.getString("difficulty", "EASY").toUpperCase(Locale.ROOT));
                } catch (Exception e) {
                    difficulty = Difficulty.EASY;
                }

                QuestType type;
                try {
                    type = QuestType.valueOf(q.getString("type", "ITEM").toUpperCase(Locale.ROOT));
                } catch (Exception e) {
                    continue;
                }

                ConfigurationSection target = q.getConfigurationSection("target");
                if (target == null) continue;

                int amount = Math.max(1, target.getInt("amount", 1));
                String targetDisplay = target.getString("display", null);

                QuestObjective objective;

                if (type == QuestType.ITEM) {
                    Material mat = Material.matchMaterial(target.getString("material", "STONE"));
                    if (mat == null) continue;
                    objective = QuestObjective.item(mat, amount, targetDisplay);
                } else if (type == QuestType.BIOME) {
                    String biome = target.getString("biome", "PLAINS");
                    NamespacedKey key = NamespacedKey.minecraft(biome.toLowerCase(Locale.ROOT));
                    if (Registry.BIOME.get(key) == null) continue;
                    objective = QuestObjective.biome(key, amount, targetDisplay);
                } else if (type == QuestType.KILL_MOB) {
                    EntityType entity;
                    try {
                        entity = EntityType.valueOf(target.getString("entity", "ZOMBIE").toUpperCase(Locale.ROOT));
                    } catch (Exception e) {
                        continue;
                    }
                    objective = QuestObjective.kill(entity, amount, targetDisplay);
                } else {
                    continue;
                }

                RewardBundle rewards = parseRewards(q.getConfigurationSection("rewards"));

                map.put(id, new QuestDefinition(id, enabled, name, description, difficulty, type, objective, rewards));
            }
        }

        this.quests = map;
    }

    public QuestDefinition get(String id) {
        return quests.get(id);
    }

    public Collection<QuestDefinition> enabledQuests() {
        List<QuestDefinition> out = new ArrayList<>();
        for (QuestDefinition q : quests.values()) {
            if (q.enabled()) out.add(q);
        }
        return out;
    }

    private RewardBundle parseRewards(ConfigurationSection sec) {
        if (sec == null) return new RewardBundle(RewardMode.ALL, List.of());

        String modeRaw = sec.getString("mode", "ALL").toUpperCase(Locale.ROOT);
        RewardMode mode;
        try {
            mode = RewardMode.valueOf(modeRaw);
        } catch (Exception e) {
            mode = RewardMode.ALL;
        }

        List<Map<?, ?>> list = sec.getMapList("options");
        List<Reward> options = new ArrayList<>();
        for (Map<?, ?> raw : list) {
            Reward r = Reward.fromMap(raw);
            if (r != null) options.add(r);
        }

        return new RewardBundle(mode, options);
    }
}
