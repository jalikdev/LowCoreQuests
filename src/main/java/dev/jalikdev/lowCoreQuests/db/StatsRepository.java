package dev.jalikdev.lowCoreQuests.db;

import dev.jalikdev.lowCore.LowCore;

import java.sql.*;
import java.util.UUID;

public class StatsRepository {

    private final LowCore core;

    public StatsRepository(LowCore core) {
        this.core = core;
    }

    public void init() {
        try (Statement st = core.getDatabaseManager().getConnection().createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS lowcore_quests_stats (
                  uuid TEXT PRIMARY KEY,
                  total_completed INTEGER NOT NULL,
                  story_completed INTEGER NOT NULL,
                  random_completed INTEGER NOT NULL
                )
                """);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Stats load(UUID uuid) {
        try (PreparedStatement ps = core.getDatabaseManager().getConnection().prepareStatement(
                "SELECT total_completed, story_completed, random_completed FROM lowcore_quests_stats WHERE uuid=?")) {

            ps.setString(1, uuid.toString());

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return new Stats(0, 0, 0);
                return new Stats(
                        rs.getInt("total_completed"),
                        rs.getInt("story_completed"),
                        rs.getInt("random_completed")
                );
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void increment(UUID uuid, boolean story) {
        try (PreparedStatement ps = core.getDatabaseManager().getConnection().prepareStatement("""
            INSERT INTO lowcore_quests_stats(uuid, total_completed, story_completed, random_completed)
            VALUES(?,?,?,?)
            ON CONFLICT(uuid) DO UPDATE SET
              total_completed = lowcore_quests_stats.total_completed + 1,
              story_completed = lowcore_quests_stats.story_completed + ?,
              random_completed = lowcore_quests_stats.random_completed + ?
            """)) {

            ps.setString(1, uuid.toString());
            ps.setInt(2, 1);
            ps.setInt(3, story ? 1 : 0);
            ps.setInt(4, story ? 0 : 1);
            ps.setInt(5, story ? 1 : 0);
            ps.setInt(6, story ? 0 : 1);

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public record Stats(int total, int story, int random) { }
}
