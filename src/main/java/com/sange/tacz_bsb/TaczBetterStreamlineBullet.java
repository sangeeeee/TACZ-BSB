package com.sange.tacz_bsb;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(TaczBetterStreamlineBullet.MOD_ID)
public final class TaczBetterStreamlineBullet {
    public static final String MOD_ID = "tacz_bsb";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TaczBetterStreamlineBullet() {
        LOGGER.info("TACZ: Better Streamline Bullet loaded");
    }
}
