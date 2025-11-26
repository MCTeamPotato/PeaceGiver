package me.kall.peacegiver;

import com.mojang.brigadier.Command;
import me.kall.duplicationless.event.BlockChangeEvent;
import me.kall.peacegiver.config.GiverConfig;
import me.kall.peacegiver.data.PeaceChunks;
import me.kall.peacegiver.ext.Giver;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

@Mod(PeaceGiver.MOD_ID)
public final class PeaceGiver {
    public static final String MOD_ID = "peacegiver";
    public static final Logger LOGGER = LogManager.getLogger(PeaceGiver.class);

    public PeaceGiver(IEventBus modBus, Dist dist, ModContainer container) {
        IEventBus forgeBus = NeoForge.EVENT_BUS;

        modBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(GiverConfig::init));
        forgeBus.addListener(this::blockChange);
        forgeBus.addListener(this::dataRebuild);
        forgeBus.addListener(this::enemySpawn);
        forgeBus.addListener(this::commandRegister);
    }

    private void blockChange(@NotNull BlockChangeEvent event) {
        ServerLevel level = event.level();
        long chunk = event.chunkPos();
        long block = event.blockPos();

        int oldRadius = ((Giver) event.oldState().getBlock()).peace$radius();
        int newRadius = ((Giver) event.newState().getBlock()).peace$radius();

        if (((Giver)event.oldState().getBlock()).peace$isGiver()) level.getServer().execute(() -> iterate(chunk, oldRadius, chunkPos -> PeaceChunks.get(level).remove(level, chunkPos, block)));
        if (((Giver)event.newState().getBlock()).peace$isGiver()) level.getServer().execute(() -> iterate(chunk, newRadius, chunkPos -> PeaceChunks.get(level).add(level, chunkPos, block)));
    }

    private void dataRebuild(@NotNull ServerStartedEvent event) {
        rebuildForServer(event.getServer());
    }

    private void enemySpawn(FinalizeSpawnEvent event) {
        if (event.isSpawnCancelled()) return;
        if (!(event.getEntity() instanceof Enemy)) return;

        ServerLevel level = event.getLevel().getLevel();
        long chunk = ChunkPos.asLong(SectionPos.blockToSectionCoord(event.getX()), SectionPos.blockToSectionCoord(event.getZ()));
        if (PeaceChunks.get(level).viewChunk(level, chunk).isEmpty()) return;
        event.setSpawnCancelled(true);
        if (GiverConfig.DEBUG) LOGGER.info("[PeaceGiver] Prevent enemy {} spawning as the chunk [{}, {}] is in peace", event.getEntity(), ChunkPos.getX(chunk), ChunkPos.getZ(chunk));
    }

    private void commandRegister(@NotNull RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("giverebuild").requires(source -> source.hasPermission(2)).executes(context -> {
            CommandSourceStack source = context.getSource();

            source.sendSuccess(() -> Component.translatable("command.giverebuild.info.start"), true);

            try {
                GiverConfig.loadConfig();
                GiverConfig.init();
                rebuildForServer(source.getServer());
                source.sendSuccess(() -> Component.translatable("command.giverebuild.info.complete"), true);
                return Command.SINGLE_SUCCESS;
            } catch (Exception e) {
                PeaceGiver.LOGGER.error("Error reloading PeaceGiver config", e);
                source.sendFailure(Component.translatable("command.giverebuild.info.error"));
                return 0;
            }
        }));
    }

    private static void iterate(long centerChunk, int radius, Consumer<Long> action) {
        int chunkX = ChunkPos.getX(centerChunk);
        int chunkZ = ChunkPos.getZ(centerChunk);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                action.accept(ChunkPos.asLong(chunkX + dx, chunkZ + dz));
            }
        }
    }

    private static void rebuildForServer(@NotNull MinecraftServer server) {
        server.execute(() -> server.getAllLevels().forEach(level -> PeaceChunks.get(level).rebuild(level)));
    }

}
