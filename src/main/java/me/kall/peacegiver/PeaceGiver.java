package me.kall.peacegiver;

import me.kall.peacegiver.config.GiverConfig;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

@Mod(PeaceGiver.MOD_ID)
public final class PeaceGiver {
    public static final String MOD_ID = "peacegiver";
    public static final String MOD_NAME = "PeaceGiver";
    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

    public PeaceGiver(@NotNull FMLJavaModLoadingContext context) {
        context.getModEventBus().addListener((FMLCommonSetupEvent event) -> event.enqueueWork(GiverConfig::init));
    }
}
