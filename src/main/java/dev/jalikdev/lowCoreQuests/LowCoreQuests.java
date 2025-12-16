package dev.jalikdev.lowCoreQuests;

import dev.jalikdev.lowCore.LowCore;
import dev.jalikdev.lowCoreQuests.command.QuestsCommand;
import dev.jalikdev.lowCoreQuests.config.QuestConfig;
import dev.jalikdev.lowCoreQuests.config.StoryConfig;
import dev.jalikdev.lowCoreQuests.db.QuestRepository;
import dev.jalikdev.lowCoreQuests.db.StoryRepository;
import dev.jalikdev.lowCoreQuests.db.StatsRepository;
import dev.jalikdev.lowCoreQuests.gui.QuestGuiListener;
import dev.jalikdev.lowCoreQuests.listener.QuestJoinListener;
import dev.jalikdev.lowCoreQuests.listener.QuestProgressListener;
import dev.jalikdev.lowCoreQuests.service.QuestService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class LowCoreQuests extends JavaPlugin {

    @Override
    public void onEnable() {
        var plugin = Bukkit.getPluginManager().getPlugin("LowCore");
        if (!(plugin instanceof LowCore core)) {
            getLogger().severe("LowCore not found. Disabling LowCoreQuests.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        QuestConfig questConfig = new QuestConfig(this, core);
        questConfig.ensureQuestsFileInLowCoreFolder();
        questConfig.reload();

        StoryConfig storyConfig = new StoryConfig(this, core);
        storyConfig.ensureStoryFileInLowCoreFolder();
        storyConfig.reload();

        QuestRepository questRepo = new QuestRepository(core);
        questRepo.init();

        StoryRepository storyRepo = new StoryRepository(core);
        storyRepo.init();

        StatsRepository statsRepo = new StatsRepository(core);
        statsRepo.init();

        QuestService service = new QuestService(core, questConfig, storyConfig, questRepo, storyRepo, statsRepo);

        getCommand("quests").setExecutor(new QuestsCommand(core, service));

        Bukkit.getPluginManager().registerEvents(new QuestGuiListener(core, service), this);
        Bukkit.getPluginManager().registerEvents(new QuestProgressListener(service), this);
        Bukkit.getPluginManager().registerEvents(new QuestJoinListener(service), this);

        for (var p : Bukkit.getOnlinePlayers()) service.load(p.getUniqueId());

        getLogger().info("LowCoreQuests enabled.");
    }
}
