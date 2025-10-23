package me.kall.peacegiver.event;

import com.mojang.brigadier.Command;
import me.kall.peacegiver.PeaceGiver;
import me.kall.peacegiver.config.GiverConfig;
import me.kall.peacegiver.data.PeaceChunks;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(modid = PeaceGiver.MOD_ID)
public class PeaceEvents {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onMobSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (event.isSpawnCancelled()) return;
        if (event.getEntity() instanceof Enemy) {
            ServerLevel level = event.getLevel().getLevel();
            long chunkPos = ChunkPos.asLong(SectionPos.blockToSectionCoord(event.getX()), SectionPos.blockToSectionCoord(event.getZ()));
            if (PeaceChunks.get(level).isPeaceChunk(level.dimension().location(), chunkPos)) {
                event.setSpawnCancelled(true);
                if (GiverConfig.DEBUG) PeaceGiver.LOGGER.info("[PeaceGiver] Prevent enemy {} spawning as the chunk [{}, {}] is in peace", event.getEntity(), ChunkPos.getX(chunkPos), ChunkPos.getZ(chunkPos));
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(@NotNull RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("giverebuild").requires(source -> source.hasPermission(2)).executes(context -> {
            CommandSourceStack source = context.getSource();

            source.sendSuccess(() -> Component.translatable("command.giverebuild.info.start"), true);

            try {
                GiverConfig.loadConfig();
                GiverConfig.init();
                PeaceChunks.rebuild(source.getServer());
                source.sendSuccess(() -> Component.translatable("command.giverebuild.info.complete"), true);
                return Command.SINGLE_SUCCESS;
            } catch (Exception e) {
                PeaceGiver.LOGGER.error("Error reloading PeaceGiver config", e);
                source.sendFailure(Component.translatable("command.giverebuild.info.error"));
                return 0;
            }
        }));
    }

    @SubscribeEvent
    public static void onServerStart(ServerStartedEvent event) {
        PeaceChunks.rebuild(event.getServer());
    }
}
