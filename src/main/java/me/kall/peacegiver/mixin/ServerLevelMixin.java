package me.kall.peacegiver.mixin;

import me.kall.peacegiver.api.Giver;
import me.kall.peacegiver.data.PeaceChunks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Shadow public abstract ServerLevel getLevel();

    @Inject(method = "onBlockStateChange", at = @At("HEAD"))
    private void onChange(BlockPos pos, BlockState oldState, BlockState newState, CallbackInfo ci) {
        Giver oldGiver = (Giver) oldState.getBlock();
        Giver newGiver = (Giver) newState.getBlock();

        long chunk = ChunkPos.asLong(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
        long blockPos = pos.asLong();

        ServerLevel level = this.getLevel();
        ResourceLocation dim = level.dimension().location();

        int oldRadius = oldGiver.peace$radius();
        int newRadius = newGiver.peace$radius();

        if (oldGiver.peace$isGiver()) level.getServer().execute(() -> {
            PeaceChunks peaceChunks = PeaceChunks.get(level);
            peaceChunks.updateRemoval(dim, oldRadius, chunk, blockPos);
        });
        if (newGiver.peace$isGiver()) level.getServer().execute(() -> {
            PeaceChunks peaceChunks = PeaceChunks.get(level);
            peaceChunks.updateAddition(dim, newRadius, chunk, blockPos);
        });
    }
}