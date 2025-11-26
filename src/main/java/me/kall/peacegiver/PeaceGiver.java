package me.kall.peacegiver;

import com.mojang.brigadier.Command;
import me.kall.duplicationless.event.BlockChangeEvent;
import me.kall.peacegiver.config.GiverConfig;
import me.kall.peacegiver.data.PeaceChunks;
import me.kall.peacegiver.ext.Giver;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

@Mod(PeaceGiver.MOD_ID)
public final class PeaceGiver {
    public static final String MOD_ID = "peacegiver";
    public static final Logger LOGGER = LogManager.getLogger(PeaceGiver.class);

    public PeaceGiver() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;

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

    @SuppressWarnings("PatternVariableCanBeUsed")
    private void enemySpawn(LivingSpawnEvent.@NotNull CheckSpawn event) {
        if (event.getResult().equals(Event.Result.DENY)) return;
        if (!(event.getEntity() instanceof Enemy) || !(event.getWorld() instanceof ServerLevel)) return;

        ServerLevel level = (ServerLevel)event.getWorld();
        long chunk = ChunkPos.asLong(SectionPos.blockToSectionCoord(Mth.floor(event.getX())), SectionPos.blockToSectionCoord(Mth.floor(event.getZ())));
        if (PeaceChunks.get(level).viewChunk(level, chunk).isEmpty()) return;
        event.setResult(Event.Result.DENY);
        if (GiverConfig.DEBUG) LOGGER.info("[PeaceGiver] Prevent enemy {} spawning as the chunk [{}, {}] is in peace", event.getEntity(), ChunkPos.getX(chunk), ChunkPos.getZ(chunk));
    }

    private void commandRegister(@NotNull RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("giverebuild").requires(source -> source.hasPermission(2)).executes(context -> {
            CommandSourceStack source = context.getSource();

            source.sendFailure(new TranslatableComponent("command.giverebuild.info.start"));

            try {
                GiverConfig.loadConfig();
                GiverConfig.init();
                rebuildForServer(source.getServer());
                source.sendSuccess(new TranslatableComponent("command.giverebuild.info.complete"), true);
                return Command.SINGLE_SUCCESS;
            } catch (Exception e) {
                PeaceGiver.LOGGER.error("Error reloading PeaceGiver config", e);
                source.sendFailure(new TranslatableComponent("command.giverebuild.info.error"));
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
