package dev.jalikdev.lowCoreQuests.util;

import org.bukkit.Location;
import org.bukkit.World;

import java.lang.reflect.Method;

public final class StructureUtil {

    private StructureUtil() {}

    public static Location extractLocation(Object result, World world) {
        if (result == null || world == null) return null;

        try {
            Method m = result.getClass().getMethod("location");
            Object loc = m.invoke(result);
            if (loc instanceof Location l) return l;
        } catch (Exception ignored) {}

        try {
            Method m = result.getClass().getMethod("getLocation");
            Object loc = m.invoke(result);
            if (loc instanceof Location l) return l;
        } catch (Exception ignored) {}

        try {
            Method m;
            try {
                m = result.getClass().getMethod("getPosition");
            } catch (NoSuchMethodException e) {
                m = result.getClass().getMethod("position");
            }

            Object pos = m.invoke(result);

            Method xM = pos.getClass().getMethod("getX");
            Method yM = pos.getClass().getMethod("getY");
            Method zM = pos.getClass().getMethod("getZ");

            int x = (int) xM.invoke(pos);
            int y = (int) yM.invoke(pos);
            int z = (int) zM.invoke(pos);

            return new Location(world, x + 0.5, y, z + 0.5);

        } catch (Exception ignored) {}

        return null;
    }
}
