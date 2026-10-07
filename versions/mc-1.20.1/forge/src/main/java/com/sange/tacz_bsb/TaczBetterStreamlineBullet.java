package com.sange.tacz_bsb;

import com.mojang.logging.LogUtils;
import com.sange.tacz_bsb.ammo.AmmoTransactions;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

@Mod(TaczBetterStreamlineBullet.MOD_ID)
public final class TaczBetterStreamlineBullet {
    public static final String MOD_ID = "tacz_bsb";
    public static final Logger LOGGER = LogUtils.getLogger();
    public TaczBetterStreamlineBullet() {
        IEventBus bus = net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();
        var container = net.minecraftforge.fml.ModLoadingContext.get();
        BsbContent.register(bus);
        container.registerConfig(ModConfig.Type.COMMON, BsbConfig.COMMON_SPEC);
        container.registerConfig(ModConfig.Type.SERVER, BsbConfig.SPEC);
        MinecraftForge.EVENT_BUS.addListener(this::commands);
    }
    private void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("tacz_bsb").then(Commands.literal("unload").executes(context -> {
            var player = context.getSource().getPlayerOrException();
            if (AmmoTransactions.ammoId(player.getMainHandItem()) == null) {
                context.getSource().sendFailure(Component.translatable("message.tacz_bsb.hold_gun"));
                return 0;
            }
            var operator = com.tacz.guns.api.entity.IGunOperator.fromLivingEntity(player);
            operator.cancelReload();
            var data = operator.getDataHolder();
            data.reloadTimestamp = -1;
            data.reloadStateType = com.tacz.guns.api.entity.ReloadState.StateType.NOT_RELOADING;
            data.isBolting = false;
            data.boltTimestamp = -1;
            AmmoTransactions.unload(player, player.getMainHandItem(), true);
            return 1;
        })));
    }
}
