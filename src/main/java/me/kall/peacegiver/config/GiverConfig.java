package me.kall.peacegiver.config;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import me.kall.duplicationless.config.JsonConfig;
import me.kall.peacegiver.PeaceGiver;
import me.kall.peacegiver.ext.Giver;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

public class GiverConfig {
    public static final Object2IntMap<String> BLOCKS = new Object2IntOpenHashMap<>();
    public static boolean DEBUG = false;

    static {
        loadConfig();
    }

    public static void loadConfig() {
        JsonConfig CONFIG = JsonConfig.create(PeaceGiver.MOD_ID, "3")
                .put("PeaceGivers", Lists.newArrayList("minecraft:beacon=3"))
                .put("DebugLogger", false)
                .initialize();
        DEBUG = CONFIG.getBoolean("DebugLogger");
        BLOCKS.clear();
        CONFIG.getStream("PeaceGivers", String.class).forEach(entry -> {
            try {
                String[] parts = entry.split("=");
                String block = parts[0];
                int radius = Integer.parseInt(parts[1]);
                BLOCKS.put(block, radius);
                PeaceGiver.LOGGER.info("[PeaceGiver] loading {} as PeaceGiver, radius {}", block, radius);
            } catch (Throwable e) {
                PeaceGiver.LOGGER.warn("Error parsing GiverConfig", e);
            }
        });
    }

    public static void init() {
        BuiltInRegistries.BLOCK.entrySet().forEach(entry -> {
            int radius = BLOCKS.getOrDefault(entry.getKey().location().toString(), -1);
            Block block = entry.getValue();
            ((Giver) block).peace$setAsGiver(radius != -1);
            ((Giver) block).peace$setRadius(radius);
        });
    }
}