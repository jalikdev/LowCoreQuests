package dev.jalikdev.lowCoreQuests.reward;

import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;

public interface Reward {
    void give(Player player);
    String display();
    Map<String, Object> toMap();

    static Reward fromMap(Map<?, ?> raw) {
        if (raw == null) return null;

        Object t = raw.get("type");
        if (t == null) return null;
        String type = String.valueOf(t).toUpperCase(Locale.ROOT);

        if (type.equals("COMMAND")) {
            Object c = raw.get("command");
            String cmd = c == null ? "" : String.valueOf(c);
            if (cmd.isBlank()) return null;
            return new CommandReward(cmd);
        }

        if (type.equals("XP")) {
            Object a = raw.get("amount");
            int amount = a instanceof Number n ? n.intValue() : parseInt(a, 0);
            if (amount <= 0) return null;
            return new XpReward(amount);
        }

        if (type.equals("ITEM")) {
            return ItemReward.fromMap(raw);
        }

        return null;
    }

    static int parseInt(Object o, int def) {
        if (o == null) return def;
        if (o instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return def; }
    }
}
