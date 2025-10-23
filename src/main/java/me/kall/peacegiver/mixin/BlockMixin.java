package me.kall.peacegiver.mixin;

import me.kall.peacegiver.api.Giver;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Block.class)
public class BlockMixin implements Giver {
    @Unique private boolean peace$isGiver;
    @Unique private int peace$radius;

    @Override
    public boolean peace$isGiver() {
        return this.peace$isGiver;
    }

    @Override
    public void peace$setAsGiver(boolean giver) {
        this.peace$isGiver = giver;
    }

    @Override
    public int peace$radius() {
        return this.peace$radius;
    }

    @Override
    public void peace$setRadius(int radius) {
        this.peace$radius = radius;
    }
}
