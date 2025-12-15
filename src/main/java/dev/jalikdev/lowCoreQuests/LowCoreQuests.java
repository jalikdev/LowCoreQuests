package dev.jalikdev.lowCoreQuests;

import dev.jalikdev.lowCore.LowCore;
import dev.jalikdev.lowCoreQuests.command.QuestsCommand;
import dev.jalikdev.lowCoreQuests.config.QuestConfig;
import dev.jalikdev.lowCoreQuests.db.QuestRepository;
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

        QuestConfig config = new QuestConfig(this, core);
        config.ensureQuestsFileInLowCoreFolder();
        config.reload();

        QuestRepository repo = new QuestRepository(core);
        repo.init();

        QuestService service = new QuestService(core, config, repo);

        getCommand("quests").setExecutor(new QuestsCommand(core, service));

        Bukkit.getPluginManager().registerEvents(new QuestGuiListener(core, service), this);
        Bukkit.getPluginManager().registerEvents(new QuestProgressListener(service), this);
        Bukkit.getPluginManager().registerEvents(new QuestJoinListener(service), this);

        for (var p : Bukkit.getOnlinePlayers()) {
            service.load(p.getUniqueId());
        }

        getLogger().info("LowCoreQuests enabled.");
    }
}
