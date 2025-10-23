package me.kall.peacegiver.data;

import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.kall.peacegiver.PeaceGiver;
import me.kall.peacegiver.api.Giver;
import me.kall.peacegiver.config.GiverConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class PeaceChunks extends SavedData {
    public final Object2ObjectMap<ResourceLocation, Long2ObjectMap<LongSet>> chunksAndReasons = new Object2ObjectOpenHashMap<>();

    public static @NotNull PeaceChunks load(@NotNull CompoundTag tag) {
        PeaceChunks data = new PeaceChunks();

        for (String dimKey : tag.getAllKeys()) {
            try {
                ResourceLocation dim = ResourceLocation.parse(dimKey);
                CompoundTag dimTag = tag.getCompound(dimKey);
                Long2ObjectMap<LongSet> chunkMap = new Long2ObjectOpenHashMap<>();

                for (String chunkKey : dimTag.getAllKeys()) {
                    long chunkLong = Long.parseLong(chunkKey);
                    long[] longs = dimTag.getLongArray(chunkKey);
                    LongSet blockSet = new LongOpenHashSet(longs);
                    chunkMap.put(chunkLong, blockSet);
                }

                data.chunksAndReasons.put(dim, chunkMap);
            } catch (Exception e) {
                PeaceGiver.LOGGER.error("Failed to load PeaceChunks for dimension {}", dimKey, e);
            }
        }

        return data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        for (Map.Entry<ResourceLocation, Long2ObjectMap<LongSet>> dimEntry : chunksAndReasons.entrySet()) {
            CompoundTag dimTag = new CompoundTag();
            dimEntry.getValue().forEach((chunkLong, blockSet) -> dimTag.putLongArray(Long.toString(chunkLong), blockSet.toLongArray()));
            tag.put(dimEntry.getKey().toString(), dimTag);
        }
        return tag;
    }

    public static @NotNull PeaceChunks get(@NotNull ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(PeaceChunks::load, PeaceChunks::new, PeaceGiver.MOD_ID + "_peace_chunks");
    }

    public void updateRemoval(ResourceLocation dim, int radius, long chunk, long blockPos) {
        Long2ObjectMap<LongSet> chunkMap = this.chunksAndReasons.get(dim);
        if (chunkMap == null) return;

        int cx = ChunkPos.getX(chunk);
        int cz = ChunkPos.getZ(chunk);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int x = cx + dx;
                int z = cz + dz;
                long affected = ChunkPos.asLong(x, z);
                LongSet set = chunkMap.get(affected);
                if (set != null) {
                    set.remove(blockPos);
                    if (set.isEmpty()) {
                        chunkMap.remove(affected);
                        if (GiverConfig.DEBUG) PeaceGiver.LOGGER.info("[PeaceGiver] Chunk [{}, {}] is no longer in peace", x, z);
                    }
                }
            }
        }

        if (chunkMap.isEmpty()) this.chunksAndReasons.remove(dim);
        this.setDirty();
    }

    public void updateAddition(ResourceLocation dim, int radius, long chunk, long blockPos) {
        Long2ObjectMap<LongSet> chunkMap = this.chunksAndReasons.computeIfAbsent(dim, key -> new Long2ObjectOpenHashMap<>());

        int cx = ChunkPos.getX(chunk);
        int cz = ChunkPos.getZ(chunk);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int x = cx + dx;
                int z = cz + dz;
                chunkMap.computeIfAbsent(ChunkPos.asLong(x, z), key -> new LongOpenHashSet()).add(blockPos);
                if (GiverConfig.DEBUG) PeaceGiver.LOGGER.info("[PeaceGiver] Chunk [{}, {}] is now in peace", x, z);
            }
        }

        this.setDirty();
    }

    public boolean isPeaceChunk(ResourceLocation dim, long chunk) {
        return !this.chunksAndReasons.getOrDefault(dim, Long2ObjectMaps.emptyMap()).getOrDefault(chunk, LongSets.emptySet()).isEmpty();
    }

    public void clear() {
        this.chunksAndReasons.clear();
        this.setDirty();
    }

    public static void rebuild(@NotNull MinecraftServer server) {
        Object2ObjectMap<ResourceLocation, LongSet> dimToGivers = new Object2ObjectOpenHashMap<>();

        server.getAllLevels().forEach(level -> PeaceChunks.get(level).chunksAndReasons.forEach((dim, chunkMap) -> chunkMap.values().forEach(dimToGivers.computeIfAbsent(dim, k -> new LongOpenHashSet())::addAll)));

        for (ServerLevel level : server.getAllLevels()) {
            PeaceChunks data = PeaceChunks.get(level);
            ResourceLocation dim = level.dimension().location();

            data.clear();

            LongSet giverList = dimToGivers.get(dim);
            if (giverList == null) continue;

            PoiManager poiManager = level.getPoiManager();

            for (long blockPos : giverList) {
                BlockPos pos = BlockPos.of(blockPos);
                poiManager.ensureLoadedAndValid(level, pos, 16);
                Giver giver = (Giver) level.getBlockState(pos).getBlock();
                if (!giver.peace$isGiver()) continue;

                int radius = giver.peace$radius();
                long chunk = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
                data.updateAddition(dim, radius, chunk, blockPos);
            }
        }
    }
}
