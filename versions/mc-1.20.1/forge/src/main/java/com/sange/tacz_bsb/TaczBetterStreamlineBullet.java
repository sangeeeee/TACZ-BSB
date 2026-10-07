package com.sange.tacz_bsb;

import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Entry point for the Forge 1.20.1 dependency foundation. */
@Mod(TaczBetterStreamlineBullet.MOD_ID)
public final class TaczBetterStreamlineBullet {
    public static final String MOD_ID = "tacz_bsb";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public TaczBetterStreamlineBullet() {
        LOGGER.info("TACZ: Better Streamline Bullet Forge 1.20.1 foundation loaded; gameplay port pending.");
    }
}
